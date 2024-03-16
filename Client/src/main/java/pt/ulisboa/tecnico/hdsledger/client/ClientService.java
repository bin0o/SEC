package pt.ulisboa.tecnico.hdsledger.client;

import com.google.gson.Gson;
import pt.ulisboa.tecnico.hdsledger.communication.*;
import pt.ulisboa.tecnico.hdsledger.utilities.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.security.PrivateKey;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;

public class ClientService {

    private static final CustomLogger LOGGER = new CustomLogger(ClientService.class.getName());
    private final ProcessConfig clientConfig;
    private final Link link;
    private final PrivateKey privateKey;
    private final Map<String, DecideMessage> receivedMessages = new ConcurrentHashMap<>();
    private final GlobalConfig config;
    private final int f;

    // Create mapping of value to frequency
    HashMap<String, Integer> frequency = new HashMap<>();
    ExecutorService threadpool = Executors.newCachedThreadPool();

    public ClientService(GlobalConfig config, Link link) {
        this.clientConfig = config.getCurrentNodeConfig();
        this.link = link;
        this.config = config;

        this.f = Math.floorDiv(config.getNodesConfigs().length - 1, 3);

        // Setup private key
        File file = new File(config.getKeysLocation() + "/Node" + this.clientConfig.getId() + "/server.key");
        try {
            String key = Files.readString(file.toPath(), Charset.defaultCharset());
            this.privateKey = CryptoUtils.parsePrivateKey(key);
        } catch (IOException e) {
            throw new HDSSException(ErrorMessage.KeyParsingFailed);
        }

        // Initialize listener
        try {
            new Thread(this::listen).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Future<DecideMessage> awaitDecide() {
    return threadpool.submit(
        () -> {
          LOGGER.log(Level.INFO, "Waiting for DECIDE responses...");

          Optional<String> value = Optional.empty();

          while (value.isEmpty()) {
            Thread.sleep(500);
            value = checkFPlusOne();
          }

          LOGGER.log(Level.INFO, MessageFormat.format("Value: {0}", value.get()));
          LOGGER.log(Level.INFO, MessageFormat.format("Received Messages: {0}", (new Gson()).toJson(receivedMessages)));

          return receivedMessages.get(value.get());
        });
    }


    public DecideMessage append(String value) throws ExecutionException, InterruptedException {
        ConsensusMessage serviceMessage = new ConsensusMessage(clientConfig.getId(), MessageType.APPEND);
        serviceMessage.setMessage((new AppendMessage(value, authenticate(value))).toJson());
        this.link.broadcast(serviceMessage);

        frequency.clear();
        receivedMessages.clear();

        Future<DecideMessage> decide = awaitDecide();

        while (!decide.isDone()) {}


        return decide.get();
    }

    public String authenticate(String value) {
        return CryptoUtils.generateSignature(value.getBytes(), this.privateKey);
    }

    private Optional<String> checkFPlusOne() {
        for (Map.Entry<String, Integer> freq : frequency.entrySet()) {
            if (freq.getValue() >= f + 1) {
                return Optional.of(freq.getKey());
            }
        }
        return Optional.empty();
    }

    private void listen() {
        try {
            while (true) {
                Message message = link.receive();

                // Separate thread to handle each message
                new Thread(() -> {
                    switch (message.getType()) {
                        case DECIDE -> {
                            ConsensusMessage consensusMessage = ((ConsensusMessage) message);
                            String value = consensusMessage.deserializeDecideMessage().getValue();
                            receivedMessages.putIfAbsent(value, consensusMessage.deserializeDecideMessage());


                            frequency.put(value, frequency.getOrDefault(value, 0) + 1);
                        }
                        default -> {
                        }
                    }
                }).start();
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
