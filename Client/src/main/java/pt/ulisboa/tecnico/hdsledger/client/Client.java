package pt.ulisboa.tecnico.hdsledger.client;

import pt.ulisboa.tecnico.hdsledger.common.models.Transaction;
import pt.ulisboa.tecnico.hdsledger.communication.ConsensusMessage;
import pt.ulisboa.tecnico.hdsledger.common.*;
import pt.ulisboa.tecnico.hdsledger.communication.Link;

import java.text.MessageFormat;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

public class Client {
  private static final CustomLogger LOGGER = new CustomLogger(Client.class.getName());

  private static final String homeMenu =
      """
            Welcome To The blockchain:
                            1-check_balance
                            2-transfer
                            3-quit""";

  public static void main(String[] args) throws ExecutionException, InterruptedException {

    String id = args[0];
    String configPath = args[1];
    boolean testMode = Boolean.parseBoolean(args[2]);

    GlobalConfig config = GlobalConfig.fromFile(configPath, id);

    if (!config.getCurrentNodeConfig().isClient())
      throw new HDSSException(ErrorMessage.NodeIsNotAClient);

    // Abstraction to send and receive messages
    Link linkToNodes = new Link(config, ConsensusMessage.class);
    ClientService clientService = new ClientService(config, linkToNodes);

    if (testMode) {
      System.exit(0);
    } else {
      Scanner sc = new Scanner(System.in);
      int input;

      while (true) {
        System.out.println(homeMenu);

        System.out.print(">> ");
        try {
          input = Integer.parseInt(sc.nextLine());
        } catch (NumberFormatException e) {
          System.out.println("Not a number");
          continue;
        }

        switch (input) {
          case 1:
            LOGGER.log(
                Level.INFO,
                MessageFormat.format(
                    "Account Balance: {0}", clientService.checkBalance().getValue()));
            break;
          case 2:
            clientService.transfer();
            break;
          case 3:
            System.exit(0);
            break;
        }
      }
    }
  }
}
