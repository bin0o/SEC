package pt.ulisboa.tecnico.hdsledger.utilities;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

public class ProcessConfigBuilder {
    private final ProcessConfig instance = new ProcessConfig();

    public ProcessConfig[] fromFile(String path) {
        System.out.println(path);
        try (BufferedInputStream is = new BufferedInputStream(new FileInputStream(path))) {
            String input = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            Gson gson = new Gson();
            ProcessConfig[] configs = gson.fromJson(input, ProcessConfig[].class);

            // Populate with public keys
            Arrays.stream(configs).forEach(processConfig -> {
                File file = new File( Global.KEYS_LOCATION + "/public" + processConfig.getId() + ".key");
                try {
                    String key = Files.readString(file.toPath(), Charset.defaultCharset());
                    processConfig.setPublicKey(CryptoUtils.parsePublicKey(key));
                } catch (Exception e) {
                    throw new HDSSException(ErrorMessage.KeyParsingFailed);
                }
            });

            return configs;
        } catch (FileNotFoundException e) {
            throw new HDSSException(ErrorMessage.ConfigFileNotFound);
        } catch (IOException | JsonSyntaxException e) {
            throw new HDSSException(ErrorMessage.ConfigFileFormat);
        }
    }

}
