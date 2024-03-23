package pt.ulisboa.tecnico.hdsledger.common.models;

import pt.ulisboa.tecnico.hdsledger.common.CryptoUtils;
import pt.ulisboa.tecnico.hdsledger.common.models.Transaction;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Block {

  private final List<Transaction> transaction;

  private final String hash;

  private final String previousBlockHash;

  public Block(List<Transaction> transaction, String previousBlockHash) {
    this.transaction = transaction;
    this.previousBlockHash = previousBlockHash;
    this.hash = calculateBlockHash();
  }

  public String getHash() {
    return hash;
  }

  private String calculateBlockHash() {
    String dataToHash;
    if (this.previousBlockHash == null) {
      dataToHash = transaction.toString();
    } else {
      dataToHash = previousBlockHash + transaction.toString();
    }

    return CryptoUtils.generateHash(dataToHash);
  }

  public List<Transaction> getTransaction() {
    return transaction;
  }

  public String getPreviousBlockHash() {
    return previousBlockHash;
  }

  @Override
  public String toString() {
    return "Block{"
        + "transaction="
        + transaction
        + ", hash='"
        + hash
        + '\''
        + ", previousBlockHash='"
        + previousBlockHash
        + '\''
        + '}';
  }
}
