package pt.ulisboa.tecnico.hdsledger.service.services;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import pt.ulisboa.tecnico.hdsledger.service.models.Account;
import pt.ulisboa.tecnico.hdsledger.common.models.Block;
import pt.ulisboa.tecnico.hdsledger.common.models.Transaction;
import pt.ulisboa.tecnico.hdsledger.communication.*;
import pt.ulisboa.tecnico.hdsledger.communication.builder.ConsensusMessageBuilder;
import pt.ulisboa.tecnico.hdsledger.service.models.*;
import pt.ulisboa.tecnico.hdsledger.common.*;

import java.security.spec.X509EncodedKeySpec;

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

  private Block inputValue;

  private List<String> clientsId;

  // Ledger (for now, just a list of strings)
  private final ArrayList<Block> ledger = new ArrayList<Block>();

  // private final Set<> receivedTransactions

  private final Map<String, Account> accounts;

  private final List<Transaction> currentTransactions;

  private final List<String> currentClients;

  private final Set<String> invalidTransactionsSignatures = new HashSet<>();

  public NodeService(Link link, GlobalConfig config) {

    this.link = link;
    this.config = config;
    this.current = config.getCurrentNodeConfig();

    this.messages = new MessageBucket(config.getServers().size());
    this.accounts = new HashMap<>();
    this.currentTransactions = new ArrayList<>();
    this.currentClients = new ArrayList<>();

    initAccounts();
  }

  private void initAccounts() {
    for (ProcessConfig client : this.config.getClients()) {
      this.accounts.put(
          Base64.getEncoder().encodeToString(client.getPublicKey().getEncoded()),
          new Account(client));
    }
  }

  public GlobalConfig getConfig() {
    return this.config;
  }

  public int getConsensusInstance() {
    return this.consensusInstance.get();
  }

  public ArrayList<Block> getLedger() {
    return this.ledger;
  }

  public ConsensusMessage createConsensusMessage(
      Block value, int instance, int round) {
    PrePrepareMessage prePrepareMessage = new PrePrepareMessage(value);

    ConsensusMessage consensusMessage =
        new ConsensusMessageBuilder(current.getId(), MessageType.PRE_PREPARE)
            .setConsensusInstance(instance)
            .setRound(round)
            .setMessage(
                config.tamperMessage(
                    instance,
                    MessageType.PRE_PREPARE,
                    PrePrepareMessage.class,
                    prePrepareMessage.toJson()))
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
  public void startConsensus(Block value, List<String> clientsId) {

    // Set initial consensus values
    this.inputValue = value;
    int localConsensusInstance = this.consensusInstance.incrementAndGet();
    this.clientsId = clientsId;
    InstanceInfo existingConsensus =
        this.instanceInfo.put(
            localConsensusInstance, new InstanceInfo(value, clientsId, config.getRoundTime()));

    // If startConsensus was already called for a given round
    if (existingConsensus != null) {
      LOGGER.log(
          Level.INFO,
          MessageFormat.format(
              "{0} - Node already started consensus for instance {1}",
              current.getId(), localConsensusInstance));
      return;
    }

    // Only start a consensus instance if the last one was decided
    // We need to be sure that the previous value has been decided
    LOGGER.log(
        Level.INFO,
        MessageFormat.format(
            "{0} - Last Decided Consensus Instance: {1}     local ConsensusInstance; {2}",
            current.getId(), lastDecidedConsensusInstance.get(), localConsensusInstance));

    while (lastDecidedConsensusInstance.get() < localConsensusInstance - 1) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    // Leader broadcasts PRE-PREPARE message
    InstanceInfo instance = this.instanceInfo.get(localConsensusInstance);

    if (this.config.isLeader(current.getId(), localConsensusInstance, instance.getCurrentRound())) {
      LOGGER.log(
          Level.INFO,
          MessageFormat.format(
              "{0} - Node is leader, sending PRE-PREPARE message", current.getId()));

      if (config.dropMessage(localConsensusInstance, MessageType.PRE_PREPARE)) return;
      // criar block
      this.link.broadcast(
          this.createConsensusMessage(
              value, localConsensusInstance, instance.getCurrentRound()));
    } else {
      LOGGER.log(
          Level.INFO,
          MessageFormat.format(
              "{0} - Node is not leader, waiting for PRE-PREPARE message", current.getId()));
    }
  }

  public void uponAppend(ConsensusMessage message) {
    AppendMessage appendMessage = message.deserializeStartMessage();

    Transaction tx = appendMessage.getValue();

    LOGGER.log(Level.INFO, MessageFormat.format("Received Transaction: {0}", tx));

    if (!isValidTransaction(tx)) {
      LOGGER.log(Level.INFO, "Invalid transaction!");
      // For tracking invalid TXs in Pre-Prepare stage
      invalidTransactionsSignatures.add(tx.getSignature());
      return;
    }

    LOGGER.log(Level.INFO, "Transaction appended");
    this.currentTransactions.add(tx);
    this.currentClients.add(message.getSenderId());

    String previousBlockHash = null;

    if (!this.ledger.isEmpty()) {
      previousBlockHash = this.ledger.get(this.ledger.size() - 1).getHash();
    }

    if (this.currentTransactions.size() == 1) {

      List<String> tempClientIds = new ArrayList<>(currentClients);
      Block block = new Block(this.currentTransactions, previousBlockHash);
      this.currentTransactions.clear();
      this.currentClients.clear();

      startConsensus(block, tempClientIds);
    }
  }

  public boolean isValidTransaction(Transaction tx) {
    byte[] publicBytes = Base64.getDecoder().decode(tx.getSource());
    X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicBytes);
    PublicKey source;

    try {
      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      source = keyFactory.generatePublic(keySpec);
    } catch (Exception e) {
      return false;
    }
    Integer amount = tx.getAmount();
    String signature = tx.getSignature();
    tx.sign(null);

    return accounts.get(tx.getSource()).getBalance() >= amount && verifyAuth(tx, signature, source);
  }

  // public boolean isValidBlock(Block block) {}

  public boolean verifyAuth(Transaction value, String signature, PublicKey publicKey) {
    return CryptoUtils.verifySignature(new Gson().toJson(value).getBytes(), signature, publicKey);
  }

  public boolean verifyAuth(Block value, String signature, PublicKey publicKey) {
    //    LOGGER.log(
    //        Level.INFO,
    //        MessageFormat.format("[DEBUG-SIGNATURE-VALIDATE]: Content: {0}", new
    // Gson().toJson(value)));
    //    LOGGER.log(
    //        Level.INFO, MessageFormat.format("[DEBUG-SIGNATURE-VALIDATE]: Signature: {0}",
    // signature));
    return CryptoUtils.verifySignature(new Gson().toJson(value).getBytes(), signature, publicKey);
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

    PrePrepareMessage prePrepareMessage = message.deserializePrePrepareMessage();

    Block value = prePrepareMessage.getBlock();



    LOGGER.log(
        Level.INFO,
        MessageFormat.format(
            "{0} - Received PRE-PREPARE message from {1} Consensus Instance {2}, Round {3}",
            current.getId(), senderId, consensusInstance, round));

    // Verify if pre-prepare was sent by leader
    if (!this.config.isLeader(senderId, consensusInstance, round)) return;


    for (Transaction tx : value.getTransaction()) {

      // I still have to validate the transaction because the leader can alter the signature and our Transaction Black List won't notice but the transaction still is invalid
      // TODO: Maybe better to do White List, and then we check if the signature is there?
      // TODO: Check previous if previous hash is compatible
      if (invalidTransactionsSignatures.contains(tx.getSignature()) && !isValidTransaction(tx)) {
        LOGGER.log(Level.INFO, MessageFormat.format("Invalid transaction!: {0}",tx.toString()));
        return;
      }
    }

    LOGGER.log(Level.INFO, "Transactions are valid");

    if (!justifyPrePrepare(round, message)) {
      LOGGER.log(Level.INFO, "PrePrepare isn't justified, ignored");
      return;
    }

    // Set instance value
    this.instanceInfo.putIfAbsent(
        consensusInstance, new InstanceInfo(value, this.clientsId, config.getRoundTime()));

    // Within an instance of the algorithm, each upon rule is triggered at most once
    // for any round r

    Optional<Collection<ConsensusMessage>> received =
        messages.getMessages(consensusInstance, round, MessageType.PRE_PREPARE);

    if (received.isPresent()) {
      LOGGER.log(
          Level.INFO,
          MessageFormat.format(
              "{0} - Already received PRE-PREPARE message for Consensus Instance {1}, Round {2}, "
                  + "replying again to make sure it reaches the initial sender",
              current.getId(), consensusInstance, round));
    } else {
      messages.addMessage(message);
    }

    PrepareMessage prepareMessage = new PrepareMessage(prePrepareMessage.getBlock());

    ConsensusMessage consensusMessage =
        new ConsensusMessageBuilder(current.getId(), MessageType.PREPARE)
            .setConsensusInstance(consensusInstance)
            .setRound(round)
            .setMessage(
                config.tamperMessage(
                    consensusInstance,
                    MessageType.PREPARE,
                    PrepareMessage.class,
                    prepareMessage.toJson()))
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

    Block value = prepareMessage.getBlock();

    LOGGER.log(
        Level.INFO,
        MessageFormat.format(
            "{0} - Received PREPARE message from {1}: Consensus Instance {2}, Round {3}",
            current.getId(), senderId, consensusInstance, round));

    // Doesn't add duplicate messages
    messages.addMessage(message);

    // Set instance values
    this.instanceInfo.putIfAbsent(
        consensusInstance, new InstanceInfo(value, this.clientsId, config.getRoundTime()));
    InstanceInfo instance = this.instanceInfo.get(consensusInstance);

    // Within an instance of the algorithm, each upon rule is triggered at most once
    // for any round r
    // Late prepare (consensus already ended for other nodes) only reply to him (as
    // an ACK)
    if (instance.getPreparedRound() >= round) {
      LOGGER.log(
          Level.INFO,
          MessageFormat.format(
              "{0} - Already received PREPARE message for Consensus Instance {1}, Round {2}, "
                  + "replying again to make sure it reaches the initial sender",
              current.getId(), consensusInstance, round));

      ConsensusMessage m =
          new ConsensusMessageBuilder(current.getId(), MessageType.COMMIT)
              .setConsensusInstance(consensusInstance)
              .setRound(round)
              .setReplyTo(senderId)
              .setReplyToMessageId(message.getMessageId())
              .setMessage(
                  config.tamperMessage(
                      consensusInstance,
                      MessageType.COMMIT,
                      CommitMessage.class,
                      instance.getCommitMessage().toJson()))
              .build();

      if (config.dropMessage(consensusInstance, MessageType.COMMIT)) return;

      link.send(senderId, m);
      return;
    }

    // Find value with valid quorum
    Optional<Block> preparedValue =
        messages.getValidQuorumValue(consensusInstance, round, MessageType.PREPARE);

    LOGGER.log(
        Level.INFO,
        MessageFormat.format(
            "[UPON_PREPARE] preparedValue is present: {0}", preparedValue.isPresent()));
    if (preparedValue.isPresent() && instance.getPreparedRound() < round) {
      instance.setPreparedValue(preparedValue.get());
      instance.setPreparedRound(round);

      CommitMessage c = new CommitMessage(preparedValue.get());
      instance.setCommitMessage(c);

      ConsensusMessage m =
          new ConsensusMessageBuilder(current.getId(), MessageType.COMMIT)
              .setConsensusInstance(consensusInstance)
              .setRound(round)
              .setReplyTo(senderId)
              .setReplyToMessageId(message.getMessageId())
              .setMessage(
                  config.tamperMessage(
                      consensusInstance, MessageType.COMMIT, CommitMessage.class, c.toJson()))
              .build();

      LOGGER.log(Level.INFO, "SENT COMMIT MESSAGE");

      if (config.dropMessage(consensusInstance, MessageType.COMMIT)) return;

      link.broadcast(m);
    }
  }

  public void initializeIBFTTimer() {
    new Thread(this::ibftTimer).start();
  }

  public void ibftInitiateRoundChange(int l) {
    InstanceInfo instance = instanceInfo.get(l);
    LOGGER.log(
        Level.INFO,
        MessageFormat.format(
            "{0} - Initiating new round: {1}", current.getId(), instance.getCurrentRound()));
    Collection<ConsensusMessage> justification;

    if (instance.getPreparedRound() != -1) {
      LOGGER.log(
          Level.INFO,
          MessageFormat.format(
              "{0} - Prepared round initiated, computing justification", current.getId()));
      // Set Justification
      justification =
          messages
              .getMessages(consensusInstance.get(), instance.getCurrentRound(), MessageType.PREPARE)
              .get();
    } else {
      justification = new ArrayList<>();
    }

    RoundChangeMessage roundChangeMessage =
        new RoundChangeMessage(
            justification, instance.getPreparedRound(), instance.getPreparedValue());

    ConsensusMessage m =
        new ConsensusMessageBuilder(current.getId(), MessageType.ROUND_CHANGE)
            .setConsensusInstance(l)
            .setRound(instance.getCurrentRound())
            .setMessage(roundChangeMessage.toJson())
            .build();

    if (config.dropMessage(l, MessageType.ROUND_CHANGE)) return;

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
          LOGGER.log(
              Level.INFO,
              MessageFormat.format(
                  "{0} - Timeout on instance {1} for round {2}",
                  current.getId(), key, instance.getCurrentRound()));

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
    new Timer()
        .schedule(
            new TimerTask() {
              public void run() {
                ibftTimer();
              }
            },
            config.getTimerInterval());
  }

  /*
   * Handle commit messages and decide if there is a valid quorum
   *
   * @param message Message to be handled
   */
  public synchronized void uponCommit(ConsensusMessage message) {
    int consensusInstance = message.getConsensusInstance();
    int round = message.getRound();

    LOGGER.log(
        Level.INFO,
        MessageFormat.format(
            "{0} - Received COMMIT message from {1}: Consensus Instance {2}, Round {3}",
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
      LOGGER.log(
          Level.INFO,
          MessageFormat.format(
              "{0} - Already received COMMIT message for Consensus Instance {1}, Round {2}, ignoring",
              current.getId(), consensusInstance, round));
      return;
    }

    Optional<Block> commitValue =
        messages.getValidQuorumValue(consensusInstance, round, MessageType.COMMIT);

    // Decide
    if (commitValue.isPresent() && instance.getCommittedRound() < round) {
      instance = this.instanceInfo.get(consensusInstance);
      instance.setCommittedRound(round);

      Block value = commitValue.get();

      // Append value to the ledger (must be synchronized to be thread-safe)
      synchronized (ledger) {

        // Increment size of ledger to accommodate current instance
        ledger.ensureCapacity(consensusInstance);
        while (ledger.size() < consensusInstance - 1) {
          ledger.add(new Block(null, null));
        }

        // TODO verify blocks function
        ledger.add(consensusInstance - 1, value);

        LOGGER.log(
            Level.INFO,
            MessageFormat.format(
                "{0} - Current Ledger: {1}",
                current.getId(), String.join((CharSequence) "", (CharSequence) ledger)));
      }

      lastDecidedConsensusInstance.getAndIncrement();

      LOGGER.log(
          Level.INFO,
          MessageFormat.format(
              "{0} - Decided on Consensus Instance {1}, Round {2}, Successful? {3}",
              current.getId(), consensusInstance, round, true));

      this.instanceInfo.get(consensusInstance).setTimer(null);

      if (this.instanceInfo.containsKey(consensusInstance)) {
        InstanceInfo info = this.instanceInfo.get(consensusInstance);

        LOGGER.log(
            Level.INFO,
            MessageFormat.format("{0} - Replying to {1}", current.getId(), info.getClientsId()));

        if (config.dropMessage(consensusInstance, MessageType.DECIDE)) return;

        LOGGER.log(Level.INFO, MessageFormat.format("[DECIDE] Value sent: {0}", value));
        // Reply to the guy who appended the block
        ConsensusMessage serviceMessage = new ConsensusMessage(current.getId(), MessageType.DECIDE);
        serviceMessage.setMessage(
            config.tamperMessage(
                consensusInstance,
                MessageType.DECIDE,
                DecideMessage.class,
                (new DecideMessage(true, consensusInstance - 1, value)).toJson()));

        for (String clientID : info.getClientsId()) {
          this.link.send(clientID, serviceMessage);
        }
      }
    }
  }

  public synchronized ConsensusMessage highestPrepared(Collection<ConsensusMessage> QRc) {
    ConsensusMessage highestPreparedMessage = null;
    for (ConsensusMessage msg : QRc) {
      RoundChangeMessage rc = msg.deserializeRoundChangeMessage();

      if ((highestPreparedMessage == null && rc.getPreparedRound() != -1)
          || (highestPreparedMessage != null
              && highestPreparedMessage.deserializeRoundChangeMessage().getPreparedRound()
                  <= rc.getPreparedRound())) {
        highestPreparedMessage = msg;
      }
    }
    return highestPreparedMessage;
  }

  private synchronized boolean validate_round_change_message(
      ConsensusMessage roundChange, ConsensusMessage highestPrepare) {
    for (ConsensusMessage consensusMessage :
        roundChange.deserializeRoundChangeMessage().getJustification()) {
      PrepareMessage prepare = consensusMessage.deserializePrepareMessage();
      if (consensusMessage.getRound() != highestPrepare.getRound()
          || !highestPrepare.deserializePrepareMessage().getBlock().equals(prepare.getBlock())) {
        return false;
      }
    }
    return true;
  }

  public synchronized boolean justifyRoundChange(Collection<ConsensusMessage> qrc) {
    ConsensusMessage highestPrepared = highestPrepared(qrc);

    if (highestPrepared == null) return true;

    for (ConsensusMessage roundChange : qrc) {
      if (roundChange.deserializeRoundChangeMessage().getPreparedRound() != -1
          || roundChange.deserializeRoundChangeMessage().getPreparedValue() != null) {
        if (!validate_round_change_message(roundChange, highestPrepared)) {
          return false;
        }
      }
    }
    return true;
  }

  public synchronized boolean justifyPrePrepare(int round, ConsensusMessage prePrepare) {
    LOGGER.log(Level.INFO, MessageFormat.format("[JUSTIFY PREPREPARE]: Round: {0}", round));

    if (round == 1) return true;

    Collection<ConsensusMessage> rcMessages =
        prePrepare.deserializePrePrepareMessage().getJustification();

    if (rcMessages.isEmpty()) return false;
    LOGGER.log(
        Level.INFO,
        MessageFormat.format(
            "[JUSTIFY PREPREPARE]: Quorum of RoundChangeMsg: {0}", new Gson().toJson(rcMessages)));
    ConsensusMessage highestRC = highestPrepared(rcMessages);

    if (highestRC == null) return true;

    for (ConsensusMessage prepareMessage :
        highestRC.deserializeRoundChangeMessage().getJustification()) {
      if (prepareMessage.getRound() != highestRC.deserializeRoundChangeMessage().getPreparedRound()
          || !highestRC
              .deserializeRoundChangeMessage()
              .getPreparedValue()
              .equals(prepareMessage.deserializePrepareMessage().getBlock())) {
        return false;
      }
    }

    return true;
  }

  public synchronized void uponRoundChange(ConsensusMessage message) {
    int consensusInstance = message.getConsensusInstance();
    int round = message.getRound();
    RoundChangeMessage roundChangeMessage = message.deserializeRoundChangeMessage();

    LOGGER.log(
        Level.INFO,
        MessageFormat.format("[ROUND_CHANGE]: Received message from: {0}", message.getSenderId()));

    // add for quorum
    messages.addMessage(message);

    // Algorithm 3 (11)
    if (this.config.isLeader(current.getId(), message.getConsensusInstance(), round)) {
      Optional<Collection<ConsensusMessage>> roundChange =
          messages.getRoundChangeQuorum(message.getConsensusInstance(), round);

      if (roundChange.isEmpty()) {
        LOGGER.log(Level.INFO, "RoundChange Quorum is empty");
        return;
      }

      ConsensusMessage highest = highestPrepared(roundChange.get());

      if (!justifyRoundChange(roundChangeMessage.getJustification())) {
        LOGGER.log(Level.INFO, "RoundChange message isn't justified, ignored");
        return;
      }

      PrePrepareMessage prePrepareMessage;

      if (highest != null) {
        prePrepareMessage =
            new PrePrepareMessage(highest.deserializeRoundChangeMessage().getPreparedValue());
      } else {
        prePrepareMessage = new PrePrepareMessage(inputValue);
      }

      prePrepareMessage.setJustification(roundChange.get());

      if (config.dropMessage(consensusInstance, MessageType.PRE_PREPARE)) return;

      ConsensusMessage consensusMessage =
          new ConsensusMessageBuilder(current.getId(), MessageType.PRE_PREPARE)
              .setConsensusInstance(consensusInstance)
              .setRound(round)
              .setMessage(
                  config.tamperMessage(
                      consensusInstance,
                      MessageType.PRE_PREPARE,
                      PrePrepareMessage.class,
                      prePrepareMessage.toJson()))
              .build();

      LOGGER.log(
          Level.INFO,
          MessageFormat.format(
              "[ROUND CHANGE] PREPREPARE message was sent with {0} roundChangeMsgs",
              roundChange.get().size()));

      link.broadcast(consensusMessage);
      return;
    }

    // Frc set
    OptionalInt roundNumber = messages.getMinRoundFromRoundChange(consensusInstance, round);

    LOGGER.log(
        Level.INFO,
        MessageFormat.format(
            "[ROUND_CHANGE]: Minimum round: {0}",
            roundNumber.isPresent() ? roundNumber.getAsInt() : "Couldn't get f + 1 ROUND_CHANGES"));

    roundNumber.ifPresent(
        integer -> {
          InstanceInfo instance = this.instanceInfo.get(consensusInstance);

          instance.setCurrentRound(integer);
          instance.setTimer(config.getRoundTime());

          RoundChangeMessage rc =
              new RoundChangeMessage(
                  messages
                      .getMessages(
                          consensusInstance, instance.getCurrentRound(), MessageType.PREPARE)
                      .get(),
                  instance.getPreparedRound(),
                  instance.getPreparedValue());
          ConsensusMessage m =
              new ConsensusMessageBuilder(current.getId(), MessageType.ROUND_CHANGE)
                  .setConsensusInstance(consensusInstance)
                  .setRound(instance.getCurrentRound())
                  .setMessage(rc.toJson())
                  .build();

          if (config.dropMessage(consensusInstance, MessageType.ROUND_CHANGE)) return;
          link.broadcast(m);
        });
  }

  @Override
  public void listen() {
    try {
      // Thread to listen on every request
      new Thread(
              () -> {
                try {
                  while (true) {
                    Message message = link.receive();

                    // Separate thread to handle each message
                    new Thread(
                            () -> {
                              switch (message.getType()) {
                                case APPEND -> uponAppend((ConsensusMessage) message);

                                case PRE_PREPARE -> uponPrePrepare((ConsensusMessage) message);

                                case PREPARE -> uponPrepare((ConsensusMessage) message);

                                case COMMIT -> uponCommit((ConsensusMessage) message);

                                case ACK ->
                                    LOGGER.log(
                                        Level.INFO,
                                        MessageFormat.format(
                                            "{0} - Received ACK message from {1}",
                                            current.getId(), message.getSenderId()));

                                case ROUND_CHANGE -> uponRoundChange((ConsensusMessage) message);

                                case IGNORE ->
                                    LOGGER.log(
                                        Level.INFO,
                                        MessageFormat.format(
                                            "{0} - Received IGNORE message from {1}",
                                            current.getId(), message.getSenderId()));

                                default ->
                                    LOGGER.log(
                                        Level.INFO,
                                        MessageFormat.format(
                                            "{0} - Received unknown message from {1}",
                                            current.getId(), message.getSenderId()));
                              }
                            })
                        .start();
                  }
                } catch (IOException | ClassNotFoundException e) {
                  e.printStackTrace();
                }
              })
          .start();

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
