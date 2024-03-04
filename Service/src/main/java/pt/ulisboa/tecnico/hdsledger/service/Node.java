package pt.ulisboa.tecnico.hdsledger.service;

import pt.ulisboa.tecnico.hdsledger.communication.ConsensusMessage;
import pt.ulisboa.tecnico.hdsledger.communication.Link;
import pt.ulisboa.tecnico.hdsledger.service.services.NodeService;
import pt.ulisboa.tecnico.hdsledger.utilities.CustomLogger;
import pt.ulisboa.tecnico.hdsledger.utilities.GlobalConfig;

import java.text.MessageFormat;
import java.util.logging.Level;

public class Node {
    private static final CustomLogger LOGGER = new CustomLogger(Node.class.getName());
    public static void main(String[] args) {

        try {
            // Command line arguments
            String id = args[0];
            String configPath = args[1];

            GlobalConfig config = GlobalConfig.fromFile(configPath, id);

            LOGGER.log(Level.INFO, MessageFormat.format("{0} - Running at {1}:{2}; is leader: {3}",
                    config.getCurrentNodeConfig().getId(),
                    config.getCurrentNodeConfig().getHostname(),
                    config.getCurrentNodeConfig().getPort(),
                    config.isLeader(config.getCurrentNodeConfig().getId(), 1, 1)));

            // Abstraction to send and receive messages
            Link linkToNodes = new Link(config, ConsensusMessage.class);

            // Services that implement listen from UDPService
            NodeService nodeService = new NodeService(linkToNodes, config);

            nodeService.initializeIBFTTimer();

            nodeService.listen();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
