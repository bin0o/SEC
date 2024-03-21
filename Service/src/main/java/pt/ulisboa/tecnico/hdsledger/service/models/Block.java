package pt.ulisboa.tecnico.hdsledger.service.models;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Block {

    private final Transaction transaction;

    private final String hash;

    private final String previousBlockHash;

    public Block(Transaction transaction, String previousBlockHash) {
        this.transaction = transaction;
        this.previousBlockHash = previousBlockHash;
        this.hash = calculateBlockHash();
    }

    public String getHash() {
        return hash;
    }

    private String calculateBlockHash() {
        String dataToHash;
        if(this.previousBlockHash == null) {
            dataToHash = transaction.toString();
        }else{
            dataToHash = previousBlockHash + transaction.toString();
        }
        MessageDigest digest = null;
        byte[] bytes = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
            bytes = digest.digest(dataToHash.getBytes(UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            System.out.println(ex.getMessage());
        }
        StringBuilder buffer = new StringBuilder();
        assert bytes != null;
        for (byte b : bytes) {
            buffer.append(String.format("%02x", b));
        }
        return buffer.toString();
    }


}
