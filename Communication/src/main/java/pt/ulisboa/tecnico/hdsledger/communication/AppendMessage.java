package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;
import pt.ulisboa.tecnico.hdsledger.common.models.Transaction;

public class AppendMessage {
  private final Transaction value;

  public AppendMessage(Transaction value) {
    this.value = value;
  }

  public Transaction getValue() {
    return value;
  }

  public String toJson() {
    return new Gson().toJson(this);
  }
}
