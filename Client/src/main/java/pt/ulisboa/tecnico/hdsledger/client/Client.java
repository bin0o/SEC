package pt.ulisboa.tecnico.hdsledger.client;

import pt.ulisboa.tecnico.hdsledger.communication.ConsensusMessage;
import pt.ulisboa.tecnico.hdsledger.communication.DecideMessage;
import pt.ulisboa.tecnico.hdsledger.service.models.Transaction;
import pt.ulisboa.tecnico.hdsledger.utilities.*;
import pt.ulisboa.tecnico.hdsledger.communication.Link;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

public class Client {
    private static final CustomLogger LOGGER = new CustomLogger(Client.class.getName());

    private static final String homeMenu = """
            Welcome To The blockchain:
                            1-check_balance
                            2-transfer
                            3-quit""";

    public static void main (String[] args) throws ExecutionException, InterruptedException {

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

           /* // Parse ledgers
            List<String> ledgers = Arrays.stream(args[3].split(",")).toList();

            int instances = config.getTestsConfigs().length;

            if (ledgers.size() != instances) {
                LOGGER.log(Level.INFO, "[Test Mode]: Error: Ledgers size and instances don't match");
                System.exit(1);
            }

            LOGGER.log(Level.INFO, MessageFormat.format("[Test Mode]: Sending payloads for {0} instances", instances));

            for(String ledger : ledgers) {
                LOGGER.log(Level.INFO, MessageFormat.format("[Test Mode]: Sending ledger -> {0}", ledger));

                DecideMessage response = clientService.append(ledger);

                if (!response.getConfirmation()) {
                    LOGGER.log(Level.INFO, MessageFormat.format("[Test Mode]: ({0}) FAIL: {1}", ledger, response.toString()));
                    System.exit(1);
                } else {
                    LOGGER.log(Level.INFO, MessageFormat.format("[Test Mode]: ({0}) OK", ledger));
                }

                Thread.sleep(1000);
            }*/

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
                        checkBalance();
                        break;
                    case 2:
                        transfer();
                        break;
                    case 3:
                        System.exit(0);
                        break;
                }
                //TODO
                /*LOGGER.log(Level.INFO, MessageFormat.format("Received: {0}", clientService.append(lol).toString()));*/
            }
        }
    }

    private static void transfer() {
        Transaction transaction;
        Scanner sc = new Scanner(System.in);
        int amount;
        String destination;
        PublicKey pubKey;

        System.out.println("Transfer funds: ");

        //check if destination is a valid PublicKey
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

        //check if amount is a number
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

        //TODO transaction
        /*transaction = new Transaction();*/

    }

    private static void checkBalance() {
    }
}
