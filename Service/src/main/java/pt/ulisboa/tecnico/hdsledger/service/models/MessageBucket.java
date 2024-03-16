package pt.ulisboa.tecnico.hdsledger.service.models;

import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import com.google.gson.Gson;
import pt.ulisboa.tecnico.hdsledger.communication.CommitMessage;
import pt.ulisboa.tecnico.hdsledger.communication.ConsensusMessage;
import pt.ulisboa.tecnico.hdsledger.communication.PrepareMessage;
import pt.ulisboa.tecnico.hdsledger.communication.RoundChangeMessage;
import pt.ulisboa.tecnico.hdsledger.utilities.CustomLogger;
import pt.ulisboa.tecnico.hdsledger.utilities.HDSSException;
import pt.ulisboa.tecnico.hdsledger.utilities.MessageType;

public class MessageBucket {
    private int f = 0;
    private static final CustomLogger LOGGER = new CustomLogger(MessageBucket.class.getName());

    // Quorum size
    private final int quorumSize;

    // Instance -> Round -> Type -> Consensus message
    private final Map<Integer, Map<Integer, Map<MessageType, Map<String, ConsensusMessage>>>> bucket = new ConcurrentHashMap<>();

    public MessageBucket(int nodeCount) {
        this.f = Math.floorDiv(nodeCount - 1, 3);
        quorumSize = Math.floorDiv(nodeCount + f, 2) + 1;
    }

    /*
     * Add a message to the bucket
     * 
     * @param consensusInstance
     * 
     * @param message
     */
    public void addMessage(ConsensusMessage message) {
        int consensusInstance = message.getConsensusInstance();
        int round = message.getRound();
        String senderId = message.getSenderId();
        MessageType type = message.getType();

        bucket.putIfAbsent(consensusInstance, new ConcurrentHashMap<>());
        bucket.get(consensusInstance).putIfAbsent(round, new ConcurrentHashMap<>());
        bucket.get(consensusInstance).get(round).putIfAbsent(type, new HashMap<>());
        bucket.get(consensusInstance).get(round).get(type).put(senderId, message);
    }

    // Behavior of round-change related methods has to be different

    public OptionalInt getMinRoundFromRoundChange(int instance, int round) {
        HashMap<String, ConsensusMessage> roundChangeSet = new HashMap<>();

        // Create set
        bucket.get(instance).forEach((roundNum, roundMsgs) -> {
            if (roundNum > round) {
                roundMsgs.get(MessageType.ROUND_CHANGE).forEach(roundChangeSet::putIfAbsent);
            }
        });

        // Get min value
        if (roundChangeSet.size() >= f + 1) {
            return roundChangeSet.values().stream().mapToInt(ConsensusMessage::getRound).min();
        } else {
            return OptionalInt.empty();
        }
    }

    public Optional<Collection<ConsensusMessage>> getRoundChangeQuorum(int instance, int round) {
        Optional<Collection<ConsensusMessage>> roundChangeQuorum = this.getMessages(instance, round, MessageType.ROUND_CHANGE);

        if (roundChangeQuorum.isPresent() && roundChangeQuorum.get().size() >= quorumSize) {
            return roundChangeQuorum;
        } else {
            return Optional.empty();
        }
    }

    private String deserializeFrequencyValue(ConsensusMessage message) {
        switch (message.getType()) {
            case PREPARE -> {
                return message.deserializePrepareMessage().getValue();
            }
            case COMMIT -> {
                return message.deserializeCommitMessage().getValue();
            }
            default -> throw new RuntimeException("UNEXPECTED deserialize type: " + message.getType().toString());
        }
    }

    public Optional<String> getValidQuorumValue(int instance, int round, MessageType type) {
        // Create mapping of value to frequency
        HashMap<String, Integer> frequency = new HashMap<>();
        bucket.get(instance).get(round).get(type).values().forEach((message) -> {
            String value = deserializeFrequencyValue(message);
            frequency.put(value, frequency.getOrDefault(value, 0) + 1);
        });

        // Only one value (if any, thus the optional) will have a frequency
        // greater than or equal to the quorum size
        return frequency.entrySet().stream().filter((Map.Entry<String, Integer> entry) -> {
            return entry.getValue() >= quorumSize;
        }).map(Map.Entry::getKey).findFirst();
    }

    public Optional<Collection<ConsensusMessage>> getMessages(int instance, int round, MessageType type) {


        if (bucket.get(instance) == null || bucket.get(instance).get(round) == null || bucket.get(instance).get(round).get(type) == null) {
            LOGGER.log(Level.INFO, MessageFormat.format("Failed to get {0} messages for instance {1}, round {2}", type.toString(), instance, round));
            LOGGER.log(Level.INFO, MessageFormat.format("Bucket content: {0}", (new Gson()).toJson(bucket)));
            return Optional.empty();
        }

        return Optional.of(bucket.get(instance).get(round).get(type).values());
    }
}