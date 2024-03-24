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
import java.util.stream.Collectors;

public class ClientService {

  private static final CustomLogger LOGGER = new CustomLogger(ClientService.class.getName());
  private final ProcessConfig clientConfig;
  private final Link link;
  private final PrivateKey privateKey;
  private final Map<String, Object> receivedMessages = new ConcurrentHashMap<>();
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

  public Future<Object> awaitDecide() {
    return threadpool.submit(
        () -> {
          LOGGER.log(Level.INFO, "Waiting for DECIDE responses...");

          Optional<String> value = Optional.empty();
          while (value.isEmpty()) {
            Thread.sleep(500);
            value = checkFPlusOne();
          }

          return receivedMessages.get(value.get());
        });
  }

  public DecideMessage append(Transaction value) throws ExecutionException, InterruptedException {
    ConsensusMessage serviceMessage =
        new ConsensusMessage(clientConfig.getId(), MessageType.APPEND);
    serviceMessage.setMessage((new AppendMessage(value)).toJson());
    this.link.broadcast(serviceMessage);

    frequency.clear();
    receivedMessages.clear();

    Future<Object> decide = awaitDecide();

    while (!decide.isDone()) {}

    if (decide.get() instanceof DecideMessage) {
      return (DecideMessage) decide.get();
    } else {
      return null;
    }
  }

  public String authenticate(Transaction value) {
    return CryptoUtils.generateSignature(new Gson().toJson(value).getBytes(), this.privateKey);
  }

  private Optional<String> checkFPlusOne() {
    for (Map.Entry<String, Integer> freq : frequency.entrySet()) {
      if (freq.getValue() >= f + 1) {
        return Optional.of(freq.getKey());
      }
    }

    return Optional.empty();
  }

  public void transfer() {
    Scanner sc = new Scanner(System.in);
    int amount;
    PublicKey pubKey = null;

    System.out.println("Transfer funds: ");
    System.out.println("--------------------------------");
    System.out.println("Available destinations(Client IDs): ");
    System.out.println();
    this.config.getClients().stream()
        .filter((client) -> !client.getId().equals(clientConfig.getId()))
        .forEach(
            (client) -> {
              System.out.println("    -- " + client.getId());
            });
    System.out.println("--------------------------------");
    while (true) {
      System.out.print("Insert client ID for Destination: ");
      ProcessConfig dest = this.config.getClientByID(sc.nextLine());
      if (dest != null) {
        pubKey = dest.getPublicKey();
        System.out.println();
        break;
      }
      System.out.println();
    }

    // check if amount is a number
    while (true) {
      System.out.print("Amount: ");
      try {
        amount = Integer.parseInt(sc.nextLine());
        System.out.println();
        if (amount <= 0) {
          System.out.println("Amount has to be a positive number different from zero");
          continue;
        }
        break;
      } catch (NumberFormatException e) {
        System.out.println();
      }
    }

    Transaction transaction = new Transaction(this.clientConfig.getPublicKey(), pubKey, amount);
    transaction.sign(authenticate(transaction));
    try {
      DecideMessage decideMessage = append(transaction);

      // trnsaction was not valid
      if (!decideMessage.getConfirmation()) {
        System.out.println("Transaction was NOT Valid");
      }
      // transaction successful
      else {
        List<Transaction> transactions =
                decideMessage.getValue().getTransaction().stream()
                        .filter(
                                (t) ->
                                        t.getSource()
                                                .equals(
                                                        Base64.getEncoder()
                                                                .encodeToString(this.clientConfig.getPublicKey().getEncoded())))
                        .collect(Collectors.toList());
        for (Transaction t : transactions) {
          System.out.println("Transaction Successful:");
          System.out.println("    amount: " + t.getAmount());
          System.out.println(
                  "    destination: " + this.config.getClientByPubKey(t.getDestination()).getId());
          System.out.println("------------------------------");
        }
      }
    } catch (Exception e) {
      System.out.println("Failed to send transaction");
    }
  }

  public BalanceReply checkBalance() throws ExecutionException, InterruptedException {
    ConsensusMessage serviceMessage =
        new ConsensusMessage(clientConfig.getId(), MessageType.CHECK_BALANCE);
    this.link.broadcast(serviceMessage);

    frequency.clear();
    receivedMessages.clear();

    Future<Object> decide = awaitDecide();

    while (!decide.isDone()) {}

    if (decide.get() instanceof BalanceReply) {
      return (BalanceReply) decide.get();
    } else {
      return null;
    }
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
                          value.getHash(), consensusMessage.deserializeDecideMessage());
                      frequency.put(
                          value.getHash(), frequency.getOrDefault(value.getHash(), 0) + 1);
                    }
                    case CHECK_BALANCE -> {
                      ConsensusMessage consensusMessage = ((ConsensusMessage) message);
                      int value = consensusMessage.deserializeBalanceReply().getValue();
                      receivedMessages.putIfAbsent(
                          String.valueOf(value), consensusMessage.deserializeBalanceReply());
                      frequency.put(
                          String.valueOf(value),
                          frequency.getOrDefault(String.valueOf(value), 0) + 1);
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
