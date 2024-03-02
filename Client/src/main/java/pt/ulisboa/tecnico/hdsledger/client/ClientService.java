package pt.ulisboa.tecnico.hdsledger.client;

import com.google.gson.Gson;
import pt.ulisboa.tecnico.hdsledger.communication.*;
import pt.ulisboa.tecnico.hdsledger.utilities.CustomLogger;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ClientService {

    private static final CustomLogger LOGGER = new CustomLogger(ClientService.class.getName());
    private final ProcessConfig clientConfig;
    private final ProcessConfig serverConfig;
    private final Link link;
    private BlockingQueue<AppendReplyMessage> replyMessagesQueue = new LinkedBlockingQueue<>();

    public ClientService(ProcessConfig clientConfig, ProcessConfig serverConfig, Link link) {
        this.clientConfig = clientConfig;
        this.serverConfig = serverConfig;
        this.link = link;

        // Initialize listener
        try {
            new Thread(this::listen).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public AppendReplyMessage append(String value) {
        ConsensusMessage serviceMessage = new ConsensusMessage(clientConfig.getId(), Message.Type.APPEND);
        serviceMessage.setMessage((new AppendMessage(value)).toJson());
        this.link.send(this.serverConfig.getId(), serviceMessage);

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
                        case APPEND_REPLY -> {
                            ConsensusMessage consensusMessage = ((ConsensusMessage) message);
                            try {
                                replyMessagesQueue.put(consensusMessage.deserializeAppendReplyMessage());
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
