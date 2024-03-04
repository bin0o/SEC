package pt.ulisboa.tecnico.hdsledger.client;

import pt.ulisboa.tecnico.hdsledger.communication.*;
import pt.ulisboa.tecnico.hdsledger.utilities.CustomLogger;
import pt.ulisboa.tecnico.hdsledger.utilities.GlobalConfig;
import pt.ulisboa.tecnico.hdsledger.utilities.MessageType;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ClientService {

    private static final CustomLogger LOGGER = new CustomLogger(ClientService.class.getName());
    private final ProcessConfig clientConfig;
    private final Link link;
    private BlockingQueue<DecideMessage> replyMessagesQueue = new LinkedBlockingQueue<>();

    public ClientService(GlobalConfig config, Link link) {
        this.clientConfig = config.getCurrentNodeConfig();
        this.link = link;

        // Initialize listener
        try {
            new Thread(this::listen).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public DecideMessage append(String value) {
        ConsensusMessage serviceMessage = new ConsensusMessage(clientConfig.getId(), MessageType.APPEND);
        serviceMessage.setMessage((new AppendMessage(value)).toJson());
        this.link.broadcast(serviceMessage);

        try {
            return replyMessagesQueue.take();
        }catch (Exception e) {
            return null;
        }
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
                            try {
                                replyMessagesQueue.put(consensusMessage.deserializeDecideMessage());
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
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
