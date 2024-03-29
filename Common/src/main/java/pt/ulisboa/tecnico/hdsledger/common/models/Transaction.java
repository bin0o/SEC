package pt.ulisboa.tecnico.hdsledger.common.models;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import pt.ulisboa.tecnico.hdsledger.common.MessageTampering;

import java.security.PublicKey;
import java.util.Base64;

public class Transaction implements MessageTampering {

  private String source;

  private String destination;

  private Integer amount;

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

  public void setSource(String source) {
    this.source = source;
  }

  public void setDestination(String destination) {
    this.destination = destination;
  }

  public void setAmount(Integer amount) {
    this.amount = amount;
  }

  public void setSignature(String signature) {
    this.signature = signature;
  }

  @Override
  public String tamperJson(JsonElement tamperData) {
    Gson gson = new Gson();

    // Create copy
    Transaction obj = gson.fromJson(gson.toJson(this), Transaction.class);

    if (tamperData.getAsJsonObject().has("source"))
      obj.setSource(tamperData.getAsJsonObject().get("source").getAsString());

    if (tamperData.getAsJsonObject().has("destination"))
      obj.setDestination(tamperData.getAsJsonObject().get("destination").getAsString());

    if (tamperData.getAsJsonObject().has("amount"))
      obj.setAmount(tamperData.getAsJsonObject().get("amount").getAsInt());

    if (tamperData.getAsJsonObject().has("signature"))
      obj.setSignature(tamperData.getAsJsonObject().get("signature").getAsString());

    if (tamperData.getAsJsonObject().has("fee"))
      obj.setFee(tamperData.getAsJsonObject().get("fee").getAsFloat());

    return gson.toJson(obj);
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
        + ", fee"
        + fee
        +   '}';
  }
}
