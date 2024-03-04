package pt.ulisboa.tecnico.hdsledger.utilities;

import java.security.PublicKey;

public class ProcessConfig {
    public ProcessConfig() {}

    private String hostname;

    private String id;

    private int port;

    private PublicKey publicKey;

    private boolean isClient;

    public int getPort() {
        return port;
    }

    public String getId() {
        return id;
    }

    public String getHostname() {
        return hostname;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public boolean isClient() {
        return isClient;
    }
}
