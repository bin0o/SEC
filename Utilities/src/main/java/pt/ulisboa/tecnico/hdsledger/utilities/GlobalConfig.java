package pt.ulisboa.tecnico.hdsledger.utilities;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import pt.ulisboa.tecnico.hdsledger.utilities.tests.State;
import pt.ulisboa.tecnico.hdsledger.utilities.tests.TestConfig;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class GlobalConfig {
    private Integer roundTime;
    private Integer timerInterval;
    private String keysLocation;
    private ProcessConfig[] nodes;
    private Map<String, ProcessConfig> nodesMap;
    private List<ProcessConfig> servers;
    private TestConfig[][] tests;
    private List<Map<String, TestConfig>> parsedTests;
    private String currentNodeId;

    private static final CustomLogger LOGGER = new CustomLogger(GlobalConfig.class.getName());

    public static GlobalConfig fromFile(String path, String id) {
        try (BufferedInputStream is = new BufferedInputStream(new FileInputStream(path))) {
            String input = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            Gson gson = new Gson();
            GlobalConfig config = gson.fromJson(input, GlobalConfig.class);

            config.setNodesMap(Arrays.stream(config.getNodesConfigs()).collect(Collectors.toMap(ProcessConfig::getId, e -> e)));
            config.setCurrentNodeId(id);

            List<ProcessConfig> servers = new ArrayList<>();

            // Populate with public keys
            Arrays.stream(config.getNodesConfigs()).forEach(processConfig -> {

                if (!processConfig.isClient())
                    servers.add(processConfig);

                File file = new File( config.getKeysLocation() + "/public" + processConfig.getId() + ".key");
                try {
                    String key = Files.readString(file.toPath(), Charset.defaultCharset());
                    processConfig.setPublicKey(CryptoUtils.parsePublicKey(key));
                } catch (Exception e) {
                    throw new HDSSException(ErrorMessage.KeyParsingFailed);
                }
            });

            config.setServers(servers);

            // Parse tests
            config.setParsedTests(Arrays.stream(config.getTestsConfigs()).map(test -> {
                if (test.length > 0) {
                    return Arrays.stream(test).collect(Collectors.toMap(TestConfig::getId, e -> e));
                } else {
                    return null;
                }
            }).collect(Collectors.toList()));

            LOGGER.log(Level.INFO, MessageFormat.format("[GlobalConfig Tests]: Parsed tests for {0} instances", config.getParsedTests().size()));

            return config;
        } catch (FileNotFoundException e) {
            throw new HDSSException(ErrorMessage.ConfigFileNotFound);
        } catch (IOException | JsonSyntaxException e) {
            System.out.println(e.getMessage());
            throw new HDSSException(ErrorMessage.ConfigFileFormat);
        }
    }

    public boolean dropMessage(Integer instance, MessageType type) {

        // Instance begins at 1
        int index = instance - 1;

        if (index >= this.parsedTests.size())
            return false;

        Map<String, TestConfig> testConfig = this.parsedTests.get(index);

        if (testConfig == null)
            return false;

        if (!testConfig.containsKey(currentNodeId))
            return false;

        State testState = testConfig.get(currentNodeId).getState();

        boolean drop = Arrays.asList(testState.getDROP()).contains(type);

        if (drop) {
            LOGGER.log(Level.WARNING, MessageFormat.format("[GlobalConfig Tests]: DROP Message -> {0} / Instance {1}", type.toString(), instance));
        }

        return drop;
    }

    private int currentLeader(Integer consensusInstance, Integer round) {
        int combinedHash = Objects.hash(consensusInstance, round);

        // Ensure the hash is non-negative
        long nonNegativeHash = combinedHash & 0xffffffffL;

        // Map the combined hash to the target range

        return (int)(nonNegativeHash % servers.size());
    }

    public boolean isLeader(String id, Integer consensusInstance, Integer round) {
        return servers.get(currentLeader(consensusInstance,round)).getId().equals(id);
    }

    public List<Map<String, TestConfig>> getParsedTests() {
        return parsedTests;
    }

    public void setParsedTests(List<Map<String, TestConfig>> parsedTests) {
        this.parsedTests = parsedTests;
    }

    public TestConfig[][] getTestsConfigs() {
        return this.tests;
    }

    public List<ProcessConfig> getServers() {
        return servers;
    }

    public void setServers(List<ProcessConfig> servers) {
        this.servers = servers;
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


}
