package pt.ulisboa.tecnico.hdsledger.utilities;

public enum ErrorMessage {
    NodeIsNotAClient("Expected this node to be a client"),
    ConfigFileNotFound("The configuration file is not available at the path supplied"),
    ConfigFileFormat("The configuration file has wrong syntax"),
    NoSuchNode("Can't send a message to a non existing node"),
    ValidationFailed("Validation of authenticity and integrity failed"),
    SignFailed("Failed to sign message"),
    KeyParsingFailed("Error while trying to parse key"),
    SocketSendingError("Error while sending message"),
    CannotOpenSocket("Error while opening socket");

    private final String message;

    ErrorMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
