package pt.ulisboa.tecnico.hdsledger.utilities;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class GlobalConfig {
    private Integer roundTime;
    private Integer timerInterval;
    private String keysLocation;
    private ProcessConfig[] nodes;
    private Map<String, ProcessConfig> nodesMap;
    private String currentNodeId;
    private String currentLeaderId;

    public static GlobalConfig fromFile(String path, String id) {
        try (BufferedInputStream is = new BufferedInputStream(new FileInputStream(path))) {
            String input = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            Gson gson = new Gson();
            GlobalConfig config = gson.fromJson(input, GlobalConfig.class);

            config.setNodesMap(Arrays.stream(config.getNodesConfigs()).collect(Collectors.toMap(ProcessConfig::getId, e -> e)));
            config.setCurrentLeaderId(Arrays.stream(config.getNodesConfigs()).filter(ProcessConfig::isLeader).findAny().get().getId());
            config.setCurrentNodeId(id);

            // Populate with public keys
            Arrays.stream(config.getNodesConfigs()).forEach(processConfig -> {
                File file = new File( config.getKeysLocation() + "/public" + processConfig.getId() + ".key");
                try {
                    String key = Files.readString(file.toPath(), Charset.defaultCharset());
                    processConfig.setPublicKey(CryptoUtils.parsePublicKey(key));
                } catch (Exception e) {
                    throw new HDSSException(ErrorMessage.KeyParsingFailed);
                }
            });

            return config;
        } catch (FileNotFoundException e) {
            throw new HDSSException(ErrorMessage.ConfigFileNotFound);
        } catch (IOException | JsonSyntaxException e) {
            throw new HDSSException(ErrorMessage.ConfigFileFormat);
        }
    }

    public void setNodesMap(Map<String, ProcessConfig> nodesMap) {
        this.nodesMap = nodesMap;
    }

    public Map<String, ProcessConfig> getNodesMap() {
        return nodesMap;
    }

    public void setCurrentNodeId(String currentNodeId) {
        this.currentNodeId = currentNodeId;
    }

    public void setCurrentLeaderId(String currentLeaderId) {
        this.currentLeaderId = currentLeaderId;
    }

    public Integer getRoundTime() {
        return roundTime;
    }

    public Integer getTimerInterval() {
        return timerInterval;
    }

    public String getKeysLocation() {
        return keysLocation;
    }

    public ProcessConfig[] getNodesConfigs() {
        return nodes;
    }

    public ProcessConfig getNodeConfig(String id) {
        return nodesMap.get(id);
    }

    public ProcessConfig getCurrentNodeConfig() {
        return this.getNodeConfig(currentNodeId);
    }

    public ProcessConfig getCurrentLeaderConfig() {
        return this.getNodeConfig(currentLeaderId);
    }
}
