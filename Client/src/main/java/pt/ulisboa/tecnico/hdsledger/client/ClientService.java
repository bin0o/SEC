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

    // Timeout system
    private boolean timeout = false;
    Timer timeoutTimer;

    private final int quorumSize;
    ExecutorService threadpool = Executors.newCachedThreadPool();

    public ClientService(GlobalConfig config, Link link) {
        this.clientConfig = config.getCurrentNodeConfig();
        this.link = link;
        this.config = config;

        int f = Math.floorDiv(config.getNodesConfigs().length - 1, 3);
        this.quorumSize = Math.floorDiv(config.getServers().size() + f, 2) + 1;

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

        // Init timeout timer
        this.timeoutTimer = new Timer();
    }

    public Future<DecideMessage> awaitDecide() {

        DecideMessage invalidDecide = new DecideMessage(false, -1, null);

        return threadpool.submit(() -> {

            LOGGER.log(Level.INFO, "Waiting for DECIDE responses...");

            while (receivedMessages.size() < quorumSize && !timeout) {}

            if (timeout) {
                LOGGER.log(Level.INFO, "Timed out while waiting for DECIDE response!");
                return invalidDecide;
            }

            // Received all the messages, stop timeout timer
            timeoutTimer.cancel();

            Optional<DecideMessage> first = receivedMessages.values().stream().findFirst();

            if (first.isEmpty()) return invalidDecide;

            LOGGER.log(Level.INFO, "Checking DECIDE responses");

            for (DecideMessage msg : receivedMessages.values()) {
                if (!msg.toJson().equals(first.get().toJson())) return invalidDecide;
            }

            return first.get();
        });
    }

    private void resetTimer() {
        timeout = false;

        timeoutTimer.cancel();

        timeoutTimer = new Timer();

        int remain = quorumSize - receivedMessages.size();
        long duration = (long) ((remain > 0) ? remain + 1 : 1) * config.getRoundTime();
        timeoutTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                timeout = true;
            }
        }, duration);
    }

    public DecideMessage append(String value) throws ExecutionException, InterruptedException {
        ConsensusMessage serviceMessage = new ConsensusMessage(clientConfig.getId(), MessageType.APPEND);
        serviceMessage.setMessage((new AppendMessage(value, authenticate(value))).toJson());
        this.link.broadcast(serviceMessage);

        receivedMessages.clear();
        resetTimer();

        Future<DecideMessage> decide = awaitDecide();

        while (!decide.isDone()) {}


        return decide.get();
    }

    public String authenticate(String value) {
        return CryptoUtils.generateSignature(value.getBytes(), this.privateKey);
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
                            receivedMessages.putIfAbsent(consensusMessage.getSenderId(), consensusMessage.deserializeDecideMessage());
                            resetTimer();
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
