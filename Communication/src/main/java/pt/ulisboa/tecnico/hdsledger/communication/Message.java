package pt.ulisboa.tecnico.hdsledger.communication;

import pt.ulisboa.tecnico.hdsledger.utilities.MessageType;

import java.io.Serializable;

public class Message implements Serializable {

    // Sender identifier
    private String senderId;
    // Message identifier
    private int messageId;
    // Message type
    private MessageType type;
    // Message Signature
    private String signature;

    public Message(String senderId, MessageType type) {
        this.senderId = senderId;
        this.type = type;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}
