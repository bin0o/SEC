package pt.ulisboa.tecnico.hdsledger.client;

import pt.ulisboa.tecnico.hdsledger.communication.AppendMessage;
import pt.ulisboa.tecnico.hdsledger.communication.ConsensusMessage;
import pt.ulisboa.tecnico.hdsledger.communication.Message;
import pt.ulisboa.tecnico.hdsledger.utilities.CustomLogger;
import pt.ulisboa.tecnico.hdsledger.communication.Link;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfigBuilder;

import java.util.Arrays;
import java.util.Scanner;

public class Client {
    private static final CustomLogger LOGGER = new CustomLogger(Client.class.getName());

    private static String nodesConfigPath = "src/main/resources/";

    public static void main (String[] args) {

        String id = args[0];
        nodesConfigPath += args[1];

        ProcessConfig[] nodeConfigs = new ProcessConfigBuilder().fromFile(nodesConfigPath);

        ProcessConfig leaderConfig = Arrays.stream(nodeConfigs).filter(ProcessConfig::isLeader).findAny().get();
        ProcessConfig currentNodeConfig = Arrays.stream(nodeConfigs).filter(c -> c.getId().equals(id)).findAny().get();

        // Abstraction to send and receive messages
        Link linkToNodes = new Link(currentNodeConfig, currentNodeConfig.getPort(), new ProcessConfig[]{leaderConfig},
                ConsensusMessage.class);


        Scanner sc = new Scanner(System.in);
        String lol;
        System.out.println("Welcome:");
        do {
            lol = sc.nextLine();

            System.out.println("Appending: " + lol);

            ConsensusMessage serviceMessage = new ConsensusMessage(id, Message.Type.APPEND);
            serviceMessage.setMessage((new AppendMessage(lol)).toJson());

            linkToNodes.send(leaderConfig.getId(), serviceMessage);
        } while (!lol.equals("quit"));
    }
}
