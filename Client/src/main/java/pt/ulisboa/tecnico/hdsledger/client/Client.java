package pt.ulisboa.tecnico.hdsledger.client;

import pt.ulisboa.tecnico.hdsledger.communication.AppendMessage;
import pt.ulisboa.tecnico.hdsledger.communication.ConsensusMessage;
import pt.ulisboa.tecnico.hdsledger.communication.Message;
import pt.ulisboa.tecnico.hdsledger.utilities.*;
import pt.ulisboa.tecnico.hdsledger.communication.Link;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Scanner;
import java.util.logging.Level;

public class Client {
    private static final CustomLogger LOGGER = new CustomLogger(Client.class.getName());

    public static void main (String[] args) {

        String id = args[0];
        String nodesConfigPath = Global.CONFIG_LOCATION + "/" + args[1];

        ProcessConfig[] nodeConfigs = new ProcessConfigBuilder().fromFile(nodesConfigPath);

        ProcessConfig leaderConfig = Arrays.stream(nodeConfigs).filter(ProcessConfig::isLeader).findAny().get();
        ProcessConfig currentNodeConfig = Arrays.stream(nodeConfigs).filter(c -> c.getId().equals(id)).findAny().get();

        if (!currentNodeConfig.isClient())
            throw new HDSSException(ErrorMessage.NodeIsNotAClient);

        // Abstraction to send and receive messages
        Link linkToNodes = new Link(currentNodeConfig, currentNodeConfig.getPort(), new ProcessConfig[]{leaderConfig},
                ConsensusMessage.class);

        ClientService clientService = new ClientService(currentNodeConfig, leaderConfig, linkToNodes);

        Scanner sc = new Scanner(System.in);
        String lol;
        System.out.println("Welcome:");

        while (true) {
            System.out.print(">> ");
            lol = sc.nextLine();

            if (lol.equals("quit")) System.exit(0);

            LOGGER.log(Level.INFO, MessageFormat.format("Received: {0}", clientService.append(lol).toString()));
        }
    }
}
