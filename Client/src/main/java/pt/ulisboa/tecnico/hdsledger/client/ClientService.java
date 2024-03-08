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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.logging.Level;

public class ClientService {

    private static final CustomLogger LOGGER = new CustomLogger(ClientService.class.getName());
    private final ProcessConfig clientConfig;
    private final Link link;
    private final PrivateKey privateKey;
    private final Map<String, DecideMessage> receivedMessages = new ConcurrentHashMap<>();
    ExecutorService threadpool = Executors.newCachedThreadPool();

    public ClientService(GlobalConfig config, Link link) {
        this.clientConfig = config.getCurrentNodeConfig();
        this.link = link;

        // Setup private key
        File file = new File( config.getKeysLocation() + "/Node" + this.clientConfig.getId() + "/server.key");
        try {
            String key = Files.readString(file.toPath(), Charset.defaultCharset());
            this.privateKey = CryptoUtils.parsePrivateKey(key);
        } catch (IOException e){
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
        return threadpool.submit(() -> {});
    }

    public DecideMessage append(String value) {
        ConsensusMessage serviceMessage = new ConsensusMessage(clientConfig.getId(), MessageType.APPEND);
        serviceMessage.setMessage((new AppendMessage(value, authenticate(value))).toJson());
        this.link.broadcast(serviceMessage);

        try {
            return replyMessagesQueue.take();
        }catch (Exception e) {
            return null;
        }
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
                        }
                        default -> {}
                    }
                }).start();
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

}
