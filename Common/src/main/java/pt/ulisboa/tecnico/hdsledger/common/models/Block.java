package pt.ulisboa.tecnico.hdsledger.common.models;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import pt.ulisboa.tecnico.hdsledger.common.CryptoUtils;
import pt.ulisboa.tecnico.hdsledger.common.MessageTampering;
import pt.ulisboa.tecnico.hdsledger.common.models.Transaction;

import java.lang.reflect.Type;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Block implements MessageTampering {

  private List<Transaction> transaction;

  private String hash;

  private String previousBlockHash;

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

  public void setTransaction (List<Transaction> transaction) { this.transaction = transaction;}

  public void setHash (String hash) {
    this.hash = hash;
  }

  public void setPreviousBlockHash(String previousBlockHash) {
    this.previousBlockHash = previousBlockHash;
  }

  @Override
  public String tamperJson(JsonElement tamperData) {
    Gson gson = new Gson();

    // Create copy
    Block obj = gson.fromJson(gson.toJson(this), Block.class);

    if (tamperData.getAsJsonObject().has("transaction")){
      JsonArray transactionArray = tamperData.getAsJsonObject().get("transaction").getAsJsonArray();

      List<Transaction> transactions = new ArrayList<>();

      for (Transaction tx : this.getTransaction()){
        int index = this.getTransaction().indexOf(tx);
        // Perform tampering operation on each transaction
        String tamperedJson = tx.tamperJson(transactionArray.get(0));

        Transaction tamperedTransaction = gson.fromJson(tamperedJson, Transaction.class);

        transactions.add(index, tamperedTransaction);
      }

      obj.setTransaction(transactions);
    }

    if (tamperData.getAsJsonObject().has("hash"))
      obj.setHash(tamperData.getAsJsonObject().get("hash").getAsString());

    if (tamperData.getAsJsonObject().has("previousBlockHash"))
      obj.setPreviousBlockHash(tamperData.getAsJsonObject().get("previousBlockHash").getAsString());

    return gson.toJson(obj);
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
