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

public class MessageBucket {

    private int f = 0;
    private static final CustomLogger LOGGER = new CustomLogger(MessageBucket.class.getName());
    // Quorum size
    private final int quorumSize;
    // Instance -> Round -> Sender ID -> Consensus message
    private final Map<Integer, Map<Integer, Map<String, ConsensusMessage>>> bucket = new ConcurrentHashMap<>();

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

        bucket.putIfAbsent(consensusInstance, new ConcurrentHashMap<>());
        bucket.get(consensusInstance).putIfAbsent(round, new ConcurrentHashMap<>());
        bucket.get(consensusInstance).get(round).put(message.getSenderId(), message);
    }

    public Optional<Integer> hasValidRoundChangeSet(int instance, int round) {
        // Create mapping of value to frequency

        HashMap<String, List<Integer>> valuePreparedRoundMap = new HashMap<>();
        HashMap<String, Integer> frequency = new HashMap<>();

        // Compute most preferred value

        bucket.get(instance).get(round).values().forEach((message) -> {
            RoundChangeMessage roundChangeMessage = message.deserializeRoundChangeMessage();
            int preparedRound = roundChangeMessage.getPreparedRound();

            if (preparedRound > round) {
                String value = roundChangeMessage.getPreparedValue();
                frequency.put(value, frequency.getOrDefault(value, 0) + 1);

                if (!valuePreparedRoundMap.containsKey(value))
                    valuePreparedRoundMap.put(value, new ArrayList<>());

                valuePreparedRoundMap.get(value).add(preparedRound);
            }
        });

        Optional<Map.Entry<String, Integer>> mostVotedPreparedValue = frequency.entrySet().stream().filter((Map.Entry<String, Integer> entry) -> {
            return entry.getValue() >= f + 1;
        }).findFirst();

        if (mostVotedPreparedValue.isEmpty()) return Optional.empty();

        LOGGER.log(Level.INFO, MessageFormat.format("Chave: {0}", mostVotedPreparedValue.get().getKey()));
        LOGGER.log(Level.INFO, MessageFormat.format("Mapa: {0}", (new Gson()).toJson(valuePreparedRoundMap)));

        return valuePreparedRoundMap.get(mostVotedPreparedValue.get().getKey()).stream().min(Integer::compareTo);
    }

    public Optional<List<ConsensusMessage>> hasValidRoundChangeQuorum(int instance, int round) {

        HashMap<String, List<ConsensusMessage>> valueMessageMap = new HashMap<>();
        HashMap<String, Integer> frequency = new HashMap<>();
        bucket.get(instance).get(round).values().forEach((message) -> {
            RoundChangeMessage roundChangeMessage = message.deserializeRoundChangeMessage();

            String value = roundChangeMessage.getPreparedValue();
            frequency.put(value, frequency.getOrDefault(value, 0) + 1);

            if (!valueMessageMap.containsKey(value))
                valueMessageMap.put(value, new ArrayList<>());
            valueMessageMap.get(value).add(message);
        });

        // Only one value (if any, thus the optional) will have a frequency
        // greater than or equal to the quorum size
        return Optional.ofNullable(valueMessageMap.get(frequency.entrySet().stream().filter((Map.Entry<String, Integer> entry) -> {
            return entry.getValue() >= quorumSize;
        }).findFirst()));
    }

    public Optional<String> hasValidPrepareQuorum(String nodeId, int instance, int round) {
        // Create mapping of value to frequency
        HashMap<String, Integer> frequency = new HashMap<>();
        bucket.get(instance).get(round).values().forEach((message) -> {
            PrepareMessage prepareMessage = message.deserializePrepareMessage();
            String value = prepareMessage.getValue();
            frequency.put(value, frequency.getOrDefault(value, 0) + 1);
        });

        // Only one value (if any, thus the optional) will have a frequency
        // greater than or equal to the quorum size
        return frequency.entrySet().stream().filter((Map.Entry<String, Integer> entry) -> {
            return entry.getValue() >= quorumSize;
        }).map((Map.Entry<String, Integer> entry) -> {
            return entry.getKey();
        }).findFirst();
    }

    public Optional<String> hasValidCommitQuorum(String nodeId, int instance, int round) {
        // Create mapping of value to frequency
        HashMap<String, Integer> frequency = new HashMap<>();
        bucket.get(instance).get(round).values().forEach((message) -> {
            CommitMessage commitMessage = message.deserializeCommitMessage();
            String value = commitMessage.getValue();
            frequency.put(value, frequency.getOrDefault(value, 0) + 1);
        });

        // Only one value (if any, thus the optional) will have a frequency
        // greater than or equal to the quorum size
        return frequency.entrySet().stream().filter((Map.Entry<String, Integer> entry) -> {
            return entry.getValue() >= quorumSize;
        }).map((Map.Entry<String, Integer> entry) -> {
            return entry.getKey();
        }).findFirst();
    }

    public Map<String, ConsensusMessage> getMessages(int instance, int round) {
        return bucket.get(instance).get(round);
    }
}