package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;

public class BalanceReply {
  private final int value;

  public BalanceReply(int value) {
    this.value = value;
  }

  public int getValue() {
    return this.value;
  }

  public String toJson() {
    return new Gson().toJson(this);
  }
}
