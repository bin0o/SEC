package pt.ulisboa.tecnico.hdsledger.common.models;

import java.security.PublicKey;
import java.util.Base64;

public class Transaction {

  private final String source;

  private final String destination;

  private final Integer amount;

  private String signature;

  private float fee;

  public Transaction(PublicKey source, PublicKey destination, Integer amount) {
    this.source = Base64.getEncoder().encodeToString(source.getEncoded());
    this.destination = Base64.getEncoder().encodeToString(destination.getEncoded());
    this.amount = amount;
  }

  public void setFee(float fee) {
    this.fee = fee;
  }

  public float getFee() {
    return this.fee;
  }

  public Transaction(String source, String destination, Integer amount) {
    this.source = source;
    this.destination = destination;
    this.amount = amount;
  }

  public void sign(String sourceSignature) {
    this.signature = sourceSignature;
  }

  public String getSource() {
    return source;
  }

  public String getDestination() {
    return destination;
  }

  public Integer getAmount() {
    return amount;
  }

  public String getSignature() {
    return String.valueOf(signature);
  }

  @Override
  public String toString() {
    return "Transaction{"
        + "source="
        + source
        + ", destination="
        + destination
        + ", amount="
        + amount
        + ", sourceSignature="
        + signature
        + '}';
  }
}
