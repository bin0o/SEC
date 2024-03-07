package pt.ulisboa.tecnico.hdsledger.service.services;

import java.io.IOException;
import java.security.PublicKey;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import com.google.gson.Gson;
import pt.ulisboa.tecnico.hdsledger.communication.*;
import pt.ulisboa.tecnico.hdsledger.communication.builder.ConsensusMessageBuilder;
import pt.ulisboa.tecnico.hdsledger.service.models.InstanceInfo;
import pt.ulisboa.tecnico.hdsledger.service.models.MessageBucket;
import pt.ulisboa.tecnico.hdsledger.utilities.*;

public class NodeService implements UDPService {

    private static final CustomLogger LOGGER = new CustomLogger(NodeService.class.getName());

    // Global configuration object
    private final GlobalConfig config;
    private final ProcessConfig current;
    // Link to communicate with nodes
    private final Link link;

    // Messages Bucket
    private final MessageBucket messages;

    // Consensus instance information per consensus instance
    private final Map<Integer, InstanceInfo> instanceInfo = new ConcurrentHashMap<>();

    // Current consensus instance
    private final AtomicInteger consensusInstance = new AtomicInteger(0);

    // Last decided consensus instance
    private final AtomicInteger lastDecidedConsensusInstance = new AtomicInteger(0);

    private String inputValue;

    // Ledger (for now, just a list of strings)
    private ArrayList<String> ledger = new ArrayList<String>();

    public NodeService(Link link, GlobalConfig config) {

        this.link = link;
        this.config = config;
        this.current = config.getCurrentNodeConfig();

        this.messages = new MessageBucket(config.getServers().size());
    }

    public GlobalConfig getConfig() {
        return this.config;
    }

    public int getConsensusInstance() {
        return this.consensusInstance.get();
    }

    public ArrayList<String> getLedger() {
        return this.ledger;
    }

    public ConsensusMessage createConsensusMessage(String value, String signedValue, int instance, int round) {
        PrePrepareMessage prePrepareMessage = new PrePrepareMessage(value);
        prePrepareMessage.setSignature(signedValue);

        ConsensusMessage consensusMessage = new ConsensusMessageBuilder(current.getId(), MessageType.PRE_PREPARE)
                .setConsensusInstance(instance)
                .setRound(round)
                .setMessage(prePrepareMessage.toJson())
                .build();

        return consensusMessage;
    }

    /*
     * Start an instance of consensus for a value
     * Only the current leader will start a consensus instance
     * the remaining nodes only update values.
     *
     * @param inputValue Value to value agreed upon
     */
    public void startConsensus(String value, String signedValue, String clientId) {

        // Set initial consensus values
        this.inputValue = value;
        int localConsensusInstance = this.consensusInstance.incrementAndGet();
        InstanceInfo existingConsensus = this.instanceInfo.put(localConsensusInstance, new InstanceInfo(value, clientId, config.getRoundTime()));

        // If startConsensus was already called for a given round
        if (existingConsensus != null) {
            LOGGER.log(Level.INFO, MessageFormat.format("{0} - Node already started consensus for instance {1}",
                    current.getId(), localConsensusInstance));
            return;
        }

        // Only start a consensus instance if the last one was decided
        // We need to be sure that the previous value has been decided
        LOGGER.log(Level.INFO, MessageFormat.format("{0} - Last Decided Consensus Instance: {1}     local ConsensusInstance; {2}",
                current.getId(), lastDecidedConsensusInstance.get(), localConsensusInstance));

        while (lastDecidedConsensusInstance.get() < localConsensusInstance - 1) {
            try {
                Thread.sleep(1000);
                LOGGER.log(Level.INFO, "SLEEPPPPPPPPPPPPPPPPPP");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Leader broadcasts PRE-PREPARE message
        InstanceInfo instance = this.instanceInfo.get(localConsensusInstance);

        if (this.config.isLeader(current.getId(), localConsensusInstance, instance.getCurrentRound())) {
            LOGGER.log(Level.INFO,
                    MessageFormat.format("{0} - Node is leader, sending PRE-PREPARE message", current.getId()));

            if (config.dropMessage(localConsensusInstance, MessageType.PRE_PREPARE)) return;

            this.link.broadcast(this.createConsensusMessage(value, signedValue, localConsensusInstance, instance.getCurrentRound()));
        } else {
            LOGGER.log(Level.INFO,
                    MessageFormat.format("{0} - Node is not leader, waiting for PRE-PREPARE message", current.getId()));
        }
    }

    public boolean verifyAuth(String value, String signature , PublicKey publicKey) {
        return CryptoUtils.verifySignature(value.getBytes(), signature, publicKey);
    }

    /*
     * Handle pre prepare messages and if the message
     * came from leader and is justified them broadcast prepare
     *
     * @param message Message to be handled
     */
    public void uponPrePrepare(ConsensusMessage message) {


        int consensusInstance = message.getConsensusInstance();
        int round = message.getRound();
        String senderId = message.getSenderId();
        int senderMessageId = message.getMessageId();

        PublicKey publicKey = this.config.getNodeConfig(this.instanceInfo.get(consensusInstance).getClientId()).getPublicKey();

        PrePrepareMessage prePrepareMessage = message.deserializePrePrepareMessage();

        String value = prePrepareMessage.getValue();
        String signedValue = prePrepareMessage.getSignature();

        if (round == 1 && !verifyAuth(value,signedValue,publicKey)){
            LOGGER.log(Level.INFO, "PrePrepare value not proposed by client");
            return;
        }

        LOGGER.log(Level.INFO, "PrePrepare value was indeed from client ---- verification successful");

        LOGGER.log(Level.INFO,
                MessageFormat.format(
                        "{0} - Received PRE-PREPARE message from {1} Consensus Instance {2}, Round {3}",
                        current.getId(), senderId, consensusInstance, round));

        // Verify if pre-prepare was sent by leader
        if (!this.config.isLeader(senderId, consensusInstance, round))
            return;

        if (round > 1) {

            Optional<Collection<ConsensusMessage>> roundChange = messages.getRoundChangeQuorum(message.getConsensusInstance(), round);

            if (roundChange.isEmpty()) {
                LOGGER.log(Level.INFO,
                        "roundChange Quorum is empty at UponPrePrepare" );
                return;
            }

            if (!justifyPrePrepare(consensusInstance, round, highestPrepared(roundChange.get()), message)) {
                LOGGER.log(Level.INFO,
                        "PrePrepare isn\'t justified, ignored");
                return;
            }
        }

        // Set instance value
        this.instanceInfo.putIfAbsent(consensusInstance, new InstanceInfo(value, null, config.getRoundTime()));

        // Within an instance of the algorithm, each upon rule is triggered at most once
        // for any round r

        Optional<Collection<ConsensusMessage>> received = messages.getMessages(consensusInstance, round, MessageType.PRE_PREPARE);


        if (received.isPresent()) {
            LOGGER.log(Level.INFO,
                    MessageFormat.format(
                            "{0} - Already received PRE-PREPARE message for Consensus Instance {1}, Round {2}, "
                                    + "replying again to make sure it reaches the initial sender",
                            current.getId(), consensusInstance, round));
        } else {
            messages.addMessage(message);
        }


        PrepareMessage prepareMessage = new PrepareMessage(prePrepareMessage.getValue());

        ConsensusMessage consensusMessage = new ConsensusMessageBuilder(current.getId(), MessageType.PREPARE)
                .setConsensusInstance(consensusInstance)
                .setRound(round)
                .setMessage(prepareMessage.toJson())
                .setReplyTo(senderId)
                .setReplyToMessageId(senderMessageId)
                .build();

        if (config.dropMessage(consensusInstance, MessageType.PREPARE)) return;

        this.link.broadcast(consensusMessage);
    }

    /*
     * Handle prepare messages and if there is a valid quorum broadcast commit
     *
     * @param message Message to be handled
     */
    public synchronized void uponPrepare(ConsensusMessage message) {

        int consensusInstance = message.getConsensusInstance();
        int round = message.getRound();
        String senderId = message.getSenderId();

        PrepareMessage prepareMessage = message.deserializePrepareMessage();

        String value = prepareMessage.getValue();

        LOGGER.log(Level.INFO,
                MessageFormat.format(
                        "{0} - Received PREPARE message from {1}: Consensus Instance {2}, Round {3}",
                        current.getId(), senderId, consensusInstance, round));

        // Doesn't add duplicate messages
        messages.addMessage(message);

        // Set instance values
        this.instanceInfo.putIfAbsent(consensusInstance, new InstanceInfo(value, null, config.getRoundTime()));
        InstanceInfo instance = this.instanceInfo.get(consensusInstance);

        // Within an instance of the algorithm, each upon rule is triggered at most once
        // for any round r
        // Late prepare (consensus already ended for other nodes) only reply to him (as
        // an ACK)
        if (instance.getPreparedRound() >= round) {
            LOGGER.log(Level.INFO,
                    MessageFormat.format(
                            "{0} - Already received PREPARE message for Consensus Instance {1}, Round {2}, "
                                    + "replying again to make sure it reaches the initial sender",
                            current.getId(), consensusInstance, round));

            ConsensusMessage m = new ConsensusMessageBuilder(current.getId(), MessageType.COMMIT)
                    .setConsensusInstance(consensusInstance)
                    .setRound(round)
                    .setReplyTo(senderId)
                    .setReplyToMessageId(message.getMessageId())
                    .setMessage(instance.getCommitMessage().toJson())
                    .build();

            if (config.dropMessage(consensusInstance, MessageType.COMMIT)) return;

            link.send(senderId, m);
            return;
        }

        // Find value with valid quorum
        Optional<String> preparedValue = messages.getValidQuorumValue(consensusInstance, round, MessageType.PREPARE);
        if (preparedValue.isPresent() && instance.getPreparedRound() < round) {
            instance.setPreparedValue(preparedValue.get());
            instance.setPreparedRound(round);
            instance.setPreparedJustificationMessage(message);

            // Must reply to prepare message senders
            Collection<ConsensusMessage> sendersMessage = messages.getMessages(consensusInstance, round, MessageType.PREPARE).get();

            CommitMessage c = new CommitMessage(preparedValue.get());
            instance.setCommitMessage(c);

            sendersMessage.forEach(senderMessage -> {
                ConsensusMessage m = new ConsensusMessageBuilder(current.getId(), MessageType.COMMIT)
                        .setConsensusInstance(consensusInstance)
                        .setRound(round)
                        .setReplyTo(senderMessage.getSenderId())
                        .setReplyToMessageId(senderMessage.getMessageId())
                        .setMessage(c.toJson())
                        .build();

                if (config.dropMessage(consensusInstance, MessageType.COMMIT)) return;

                link.broadcast(m);
            });
        }
    }


    public void initializeIBFTTimer() {
        new Thread(this::ibftTimer).start();
    }

    public void ibftInitiateRoundChange(int l) {
        InstanceInfo instance = instanceInfo.get(l);
        LOGGER.log(Level.INFO, MessageFormat.format("{0} - Initiating new round: {1}", current.getId(), instance.getCurrentRound()));
        ConsensusMessage justification;

        if (instance.getPreparedRound() != -1) {
            LOGGER.log(Level.INFO, MessageFormat.format("{0} - Prepared round initiated, computing justification", current.getId()));
            // Set Justification
            justification = instance.getPreparedJustificationMessage();
        } else {
            justification = null;
        }

        RoundChangeMessage roundChangeMessage = new RoundChangeMessage(justification, instance.getPreparedRound(), instance.getPreparedValue());
        ConsensusMessage m = new ConsensusMessageBuilder(current.getId(), MessageType.ROUND_CHANGE)
                .setConsensusInstance(l)
                .setRound(instance.getCurrentRound())
                .setMessage(roundChangeMessage.toJson())
                .build();
        link.broadcast(m);
    }

    private void ibftTimer() {
        for (Map.Entry<Integer, InstanceInfo> entry : instanceInfo.entrySet()) {
            int key = entry.getKey();
            InstanceInfo instance = entry.getValue();
            if (instance.getTimer() != null) {
                // Get timer
                int timer = instance.getTimer();
                timer -= config.getTimerInterval();
                if (timer <= 0) {
                    LOGGER.log(Level.INFO, MessageFormat.format("{0} - Timeout on instance {1} for round {2}", current.getId(), key, instance.getCurrentRound()));

                    // Update round
                    instance.setCurrentRound(instance.getCurrentRound() + 1);
                    instance.setTimer(config.getRoundTime() * (int) Math.pow(2, instance.getCurrentRound()));

                    // Init Round Change
                    ibftInitiateRoundChange(key);
                } else {
                    instance.setTimer(timer);
                }
            }
        }
        new Timer().schedule(new TimerTask() {
            public void run() {
                ibftTimer();
            }
        }, config.getTimerInterval());
    }

    /*
     * Handle commit messages and decide if there is a valid quorum
     *
     * @param message Message to be handled
     */
    public synchronized void uponCommit(ConsensusMessage message) {
        int consensusInstance = message.getConsensusInstance();
        int round = message.getRound();

        LOGGER.log(Level.INFO,
                MessageFormat.format("{0} - Received COMMIT message from {1}: Consensus Instance {2}, Round {3}",
                        current.getId(), message.getSenderId(), consensusInstance, round));

        messages.addMessage(message);

        InstanceInfo instance = this.instanceInfo.get(consensusInstance);

        if (instance == null) {
            // Should never happen because only receives commit as a response to a prepare message
            MessageFormat.format(
                    "{0} - CRITICAL: Received COMMIT message from {1}: Consensus Instance {2}, Round {3} BUT NO INSTANCE INFO",
                    current.getId(), message.getSenderId(), consensusInstance, round);
            return;
        }

        // Within an instance of the algorithm, each upon rule is triggered at most once
        // for any round r
        if (instance.getCommittedRound() >= round) {
            LOGGER.log(Level.INFO,
                    MessageFormat.format(
                            "{0} - Already received COMMIT message for Consensus Instance {1}, Round {2}, ignoring",
                            current.getId(), consensusInstance, round));
            return;
        }

        Optional<String> commitValue = messages.getValidQuorumValue(consensusInstance, round, MessageType.COMMIT);

        // Decide
        if (commitValue.isPresent() && instance.getCommittedRound() < round) {
            instance = this.instanceInfo.get(consensusInstance);
            instance.setCommittedRound(round);

            String value = commitValue.get();

            // Append value to the ledger (must be synchronized to be thread-safe)
            synchronized (ledger) {

                // Increment size of ledger to accommodate current instance
                ledger.ensureCapacity(consensusInstance);
                while (ledger.size() < consensusInstance - 1) {
                    ledger.add("");
                }

                ledger.add(consensusInstance - 1, value);

                LOGGER.log(Level.INFO,
                        MessageFormat.format(
                                "{0} - Current Ledger: {1}",
                                current.getId(), String.join("", ledger)));
            }

            lastDecidedConsensusInstance.getAndIncrement();

            LOGGER.log(Level.INFO,
                    MessageFormat.format(
                            "{0} - Decided on Consensus Instance {1}, Round {2}, Successful? {3}",
                            current.getId(), consensusInstance, round, true));

            this.instanceInfo.get(consensusInstance).setTimer(null);

            if (this.config.isLeader(current.getId(), consensusInstance, round) && this.instanceInfo.containsKey(consensusInstance)) {
                InstanceInfo info = this.instanceInfo.get(consensusInstance);

                LOGGER.log(Level.INFO,
                        MessageFormat.format(
                                "{0} - Replying to {1}",
                                current.getId(), info.getClientId()));

                if (config.dropMessage(consensusInstance, MessageType.DECIDE)) return;

                // Reply to the guy who appended the block
                ConsensusMessage serviceMessage = new ConsensusMessage(current.getId(), MessageType.DECIDE);
                serviceMessage.setMessage((new DecideMessage(true, consensusInstance - 1)).toJson());
                this.link.send(info.getClientId(), serviceMessage);
            }
        }
    }

    public synchronized ConsensusMessage highestPrepared(Collection<ConsensusMessage> QRc) {
        ConsensusMessage highestPreparedMessage = null;
        for (ConsensusMessage msg : QRc) {
            RoundChangeMessage rc = msg.deserializeRoundChangeMessage();

            if ((highestPreparedMessage == null && rc.getPreparedRound() != -1) || (highestPreparedMessage != null && highestPreparedMessage.deserializeRoundChangeMessage().getPreparedRound() <= rc.getPreparedRound())) {
                highestPreparedMessage = msg;
            }
        }
        return highestPreparedMessage;
    }

    public synchronized boolean justifyRoundChange(int instance, int round, ConsensusMessage highestPrepared, String value) {

        if (highestPrepared == null) {
            LOGGER.log(Level.INFO,"[JUSTIFY ROUND CHANGE]: highestPrepared == null, so TRUE");
            return true;
        }

        LOGGER.log(Level.INFO,"[JUSTIFY ROUND CHANGE]: before loop");

        for (ConsensusMessage entry : messages.getMessages(instance, round, MessageType.PREPARE).get()) {

            LOGGER.log(Level.INFO,"[JUSTIFY ROUND CHANGE]: going to the loop");

            String val = (value == null) ? entry.deserializePrepareMessage().getValue() : value;

            if (!val.equals(highestPrepared.deserializeRoundChangeMessage().getPreparedValue())) {
                return false;
            }
            if (entry.getRound() != highestPrepared.deserializeRoundChangeMessage().getPreparedRound()) {
                return false;
            }
        }
        return true;
    }

    public synchronized boolean justifyRoundChange(int instance, int round, ConsensusMessage highestPrepared) {
        return justifyRoundChange(instance, round, highestPrepared, null);
    }

    public synchronized boolean justifyPrePrepare(int instance, int round, ConsensusMessage highestPrepared, ConsensusMessage unparsedPrePrepare) {
        LOGGER.log(Level.INFO, MessageFormat.format("[JUSTIFY PREPREPARE]: Round: {0}", round));
        return (round == 1) || justifyRoundChange(instance, round, highestPrepared, unparsedPrePrepare.deserializePrePrepareMessage().getValue());
    }

    public synchronized void uponRoundChange(ConsensusMessage message) {
        int consensusInstance = message.getConsensusInstance();
        int round = message.getRound();
        RoundChangeMessage roundChangeMessage = message.deserializeRoundChangeMessage();

        LOGGER.log(Level.INFO, MessageFormat.format("[ROUND_CHANGE]: Received message from: {0}", message.getSenderId()));

        //add for quorum
        messages.addMessage(message);

        // Frc set
        OptionalInt roundNumber = messages.getMinRoundFromRoundChange(consensusInstance, round);

        LOGGER.log(Level.INFO, MessageFormat.format("[ROUND_CHANGE]: Minimum round: {0}", roundNumber.isPresent() ? roundNumber.getAsInt() : "Couldn't get f + 1 ROUND_CHANGES"));

        AtomicBoolean broadcasted = new AtomicBoolean(false);

        roundNumber.ifPresent(integer -> {
            InstanceInfo instance = this.instanceInfo.get(consensusInstance);

            broadcasted.set(true);

            instance.setCurrentRound(integer);
            instance.setTimer(config.getRoundTime());

            RoundChangeMessage rc = new RoundChangeMessage(instance.getPreparedJustificationMessage(), instance.getPreparedRound(), instance.getPreparedValue());
            ConsensusMessage m = new ConsensusMessageBuilder(current.getId(), MessageType.ROUND_CHANGE)
                    .setConsensusInstance(consensusInstance)
                    .setRound(instance.getCurrentRound())
                    .setMessage(rc.toJson())
                    .build();

            if (config.dropMessage(consensusInstance, MessageType.ROUND_CHANGE)) return;
            link.broadcast(m);
        });

        if (broadcasted.get()) return;

        // Algorithm 3 (11)
        if (this.config.isLeader(current.getId(), message.getConsensusInstance(), round)) {
            Optional<Collection<ConsensusMessage>> roundChange = messages.getRoundChangeQuorum(message.getConsensusInstance(), round);

            if (roundChange.isEmpty()) {
                LOGGER.log(Level.INFO,
                        "RoundChange Quorum is empty");
                return;
            }


            ConsensusMessage highest = highestPrepared(roundChange.get());

            if (!justifyRoundChange(message.getConsensusInstance(), round, highest)) {
                LOGGER.log(Level.INFO,
                        "RoundChange message isn\'t justified, ignored");
                return;
            }

            PrePrepareMessage prePrepareMessage;

            if (highest != null) {
                prePrepareMessage = new PrePrepareMessage(highest.deserializeRoundChangeMessage().getPreparedValue());
            } else {
                prePrepareMessage = new PrePrepareMessage(inputValue);
            }

            if (config.dropMessage(consensusInstance, MessageType.PRE_PREPARE)) return;
            ConsensusMessage consensusMessage = new ConsensusMessageBuilder(current.getId(), MessageType.PRE_PREPARE)
                    .setConsensusInstance(consensusInstance)
                    .setRound(round)
                    .setMessage(prePrepareMessage.toJson())
                    .build();

            link.broadcast(consensusMessage);
        }
    }

    @Override
    public void listen() {
        try {
            // Thread to listen on every request
            new Thread(() -> {
                try {
                    while (true) {
                        Message message = link.receive();

                        // Separate thread to handle each message
                        new Thread(() -> {

                            switch (message.getType()) {

                                case APPEND -> {
                                    ConsensusMessage consensusMessage = ((ConsensusMessage) message);
                                    AppendMessage appendMessage = consensusMessage.deserializeStartMessage();
                                    startConsensus(appendMessage.getValue(), appendMessage.getJustification(), message.getSenderId());
                                }

                                case PRE_PREPARE -> uponPrePrepare((ConsensusMessage) message);

                                case PREPARE -> uponPrepare((ConsensusMessage) message);

                                case COMMIT -> uponCommit((ConsensusMessage) message);

                                case ACK ->
                                        LOGGER.log(Level.INFO, MessageFormat.format("{0} - Received ACK message from {1}",
                                                current.getId(), message.getSenderId()));

                                case ROUND_CHANGE -> uponRoundChange((ConsensusMessage) message);

                                case IGNORE -> LOGGER.log(Level.INFO,
                                        MessageFormat.format("{0} - Received IGNORE message from {1}",
                                                current.getId(), message.getSenderId()));

                                default -> LOGGER.log(Level.INFO,
                                        MessageFormat.format("{0} - Received unknown message from {1}",
                                                current.getId(), message.getSenderId()));

                            }

                        }).start();
                    }
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
