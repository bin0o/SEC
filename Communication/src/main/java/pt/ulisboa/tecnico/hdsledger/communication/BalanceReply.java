package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;

public class BalanceReply {
  private final float value;

  public BalanceReply(float value) {
    this.value = value;
  }

  public float getValue() {
    return this.value;
  }

  public String toJson() {
    return new Gson().toJson(this);
  }
}
