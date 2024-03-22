package pt.ulisboa.tecnico.hdsledger.client;

import com.google.gson.Gson;
import pt.ulisboa.tecnico.hdsledger.common.models.Block;
import pt.ulisboa.tecnico.hdsledger.communication.*;
import pt.ulisboa.tecnico.hdsledger.common.models.Transaction;
import pt.ulisboa.tecnico.hdsledger.common.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;

public class ClientService {

  private static final CustomLogger LOGGER = new CustomLogger(ClientService.class.getName());
  private final ProcessConfig clientConfig;
  private final Link link;
  private final PrivateKey privateKey;
  private final Map<Block, DecideMessage> receivedMessages = new ConcurrentHashMap<>();
  private final Map<Integer, BalanceReply> receivedMessagesBalance = new ConcurrentHashMap<>();
  private final GlobalConfig config;
  private final int f;

  // Create mapping of value to frequency
  HashMap<Block, Integer> frequencyTransfer = new HashMap<>();

  HashMap<Integer, Integer> frequencyBalance = new HashMap<>();
  ExecutorService threadpool = Executors.newCachedThreadPool();

  public ClientService(GlobalConfig config, Link link) {
    this.clientConfig = config.getCurrentNodeConfig();
    this.link = link;
    this.config = config;

    this.f = Math.floorDiv(config.getNodesConfigs().length - 1, 3);

    // Setup private key
    File file =
        new File(config.getKeysLocation() + "/Node" + this.clientConfig.getId() + "/server.key");
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

  public Future<DecideMessage> awaitDecideTransfer() {
    return threadpool.submit(
        () -> {
          LOGGER.log(Level.INFO, "Waiting for DECIDE responses...");

          Optional<Block> value = Optional.empty();
          while (value.isEmpty()) {
            Thread.sleep(500);
            value = checkFPlusOneTransfer();
          }

          LOGGER.log(Level.INFO, MessageFormat.format("Value: {0}", value.get()));
          LOGGER.log(
              Level.INFO,
              MessageFormat.format(
                  "Received Messages: {0}", (new Gson()).toJson(receivedMessages)));

          return receivedMessages.get(value.get());
        });
  }

  public Future<BalanceReply> awaitDecideCheckBalance() {
    return threadpool.submit(
        () -> {
          LOGGER.log(Level.INFO, "Waiting for Check_Balance responses...");

          Optional<Integer> value = Optional.empty();

          while (value.isEmpty()) {
            Thread.sleep(500);
            value = checkFPlusOneCheckBalance();
          }

          LOGGER.log(Level.INFO, MessageFormat.format("Value: {0}", value.get()));
          LOGGER.log(
              Level.INFO,
              MessageFormat.format(
                  "Received Messages: {0}", (new Gson()).toJson(receivedMessagesBalance)));

          return receivedMessagesBalance.get(value.get());
        });
  }

  public DecideMessage append(Transaction value) throws ExecutionException, InterruptedException {
    ConsensusMessage serviceMessage =
        new ConsensusMessage(clientConfig.getId(), MessageType.APPEND);
    serviceMessage.setMessage((new AppendMessage(value)).toJson());
    this.link.broadcast(serviceMessage);

    frequencyTransfer.clear();
    receivedMessages.clear();

    Future<DecideMessage> decide = awaitDecideTransfer();

    while (!decide.isDone()) {}

    return decide.get();
  }

  public String authenticate(Transaction value) {
    return CryptoUtils.generateSignature(new Gson().toJson(value).getBytes(), this.privateKey);
  }

  private Optional<Block> checkFPlusOneTransfer() {
    for (Map.Entry<Block, Integer> freq : frequencyTransfer.entrySet()) {
      if (freq.getValue() >= f + 1) {
        return Optional.of(freq.getKey());
      }
    }
    return Optional.empty();
  }

  private Optional<Integer> checkFPlusOneCheckBalance() {
    for (Map.Entry<Integer, Integer> freq : frequencyBalance.entrySet()) {
      if (freq.getValue() >= f + 1) {
        return Optional.of(freq.getKey());
      }
    }
    return Optional.empty();
  }

  public Block transfer() {
    Scanner sc = new Scanner(System.in);
    int amount;
    String destination;
    PublicKey pubKey;

    System.out.println("Transfer funds: ");

    System.out.println("Available destinations: ");
    System.out.println();
    this.config.getClients().stream()
        .filter((client) -> !client.getId().equals(clientConfig.getId()))
        .forEach(
            (client) -> {
              System.out.println(client.getId());
              System.out.println(
                  Base64.getEncoder().encodeToString(client.getPublicKey().getEncoded()));
              System.out.println();
            });

    // check if destination is a valid PublicKey
    while (true) {
      System.out.print("Destination: ");
      destination = sc.nextLine();
      try {
        byte[] publicBytes = Base64.getDecoder().decode(destination);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        pubKey = keyFactory.generatePublic(keySpec);
        System.out.println();
        break;
      } catch (Exception e) {
        System.out.println();
      }
    }
    // check if amount is a number
    while (true) {
      System.out.print("Amount: ");
      try {
        amount = Integer.parseInt(sc.nextLine());
        System.out.println();
        break;
      } catch (NumberFormatException e) {
        System.out.println();
      }
    }

    Transaction transaction = new Transaction(this.clientConfig.getPublicKey(), pubKey, amount);
    transaction.sign(authenticate(transaction));
    try {
      return append(transaction).getValue();
    } catch (Exception e) {
      System.out.println("Failed to send transaction");
    }
    return null;
  }

  public BalanceReply checkBalance() throws ExecutionException, InterruptedException {
    ConsensusMessage serviceMessage =
        new ConsensusMessage(clientConfig.getId(), MessageType.CHECK_BALANCE);
    this.link.broadcast(serviceMessage);

    frequencyBalance.clear();
    receivedMessagesBalance.clear();

    Future<BalanceReply> decide = awaitDecideCheckBalance();

    while (!decide.isDone()) {}

    return decide.get();
  }

  private void listen() {
    try {
      while (true) {
        Message message = link.receive();

        // Separate thread to handle each message
        new Thread(
                () -> {
                  switch (message.getType()) {
                    case DECIDE -> {
                      ConsensusMessage consensusMessage = ((ConsensusMessage) message);
                      Block value = consensusMessage.deserializeDecideMessage().getValue();
                      receivedMessages.putIfAbsent(
                          value, consensusMessage.deserializeDecideMessage());

                      frequencyTransfer.put(value, frequencyTransfer.getOrDefault(value, 0) + 1);
                    }
                    case CHECK_BALANCE -> {
                      ConsensusMessage consensusMessage = ((ConsensusMessage) message);
                      int value = consensusMessage.deserializeBalanceReply().getValue();
                      receivedMessagesBalance.putIfAbsent(
                          value, consensusMessage.deserializeBalanceReply());
                      frequencyBalance.put(value, frequencyBalance.getOrDefault(value, 0) + 1);
                    }
                    default -> {}
                  }
                })
            .start();
      }
    } catch (IOException | ClassNotFoundException e) {
      e.printStackTrace();
    }
  }
}
