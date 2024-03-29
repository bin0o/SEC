package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import pt.ulisboa.tecnico.hdsledger.common.MessageTampering;
import pt.ulisboa.tecnico.hdsledger.common.models.Transaction;

public class AppendMessage implements MessageTampering {
  private Transaction value;

  public AppendMessage(Transaction value) {
    this.value = value;
  }

  public Transaction getValue() {
    return value;
  }

  public void setValue(Transaction value) {
    this.value = value;
  }

  public String toJson() {
    return new Gson().toJson(this);
  }

  @Override
  public String tamperJson(JsonElement tamperData) {
    Gson gson = new Gson();

    // Create copy
    AppendMessage obj = gson.fromJson(this.toJson(), AppendMessage.class);

    if (tamperData.getAsJsonObject().has("value")) {
      obj.setValue(
          gson.fromJson(
              this.getValue().tamperJson(tamperData.getAsJsonObject().get("value")),
              Transaction.class));
    }

    return obj.toJson();
  }
}
