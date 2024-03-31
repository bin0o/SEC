package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import pt.ulisboa.tecnico.hdsledger.common.MessageTampering;
import pt.ulisboa.tecnico.hdsledger.common.models.Transaction;

public class BalanceReply implements MessageTampering {
  private float value;

  public BalanceReply(float value) {
    this.value = value;
  }

  public float getValue() {
    return this.value;
  }

  public void setValue(float value) {this.value = value;}

  public String toJson() {
    return new Gson().toJson(this);
  }

  @Override
  public String tamperJson(JsonElement tamperData) {
    Gson gson = new Gson();

    // Create copy
    BalanceReply obj = gson.fromJson(this.toJson(), BalanceReply.class);

    if (tamperData.getAsJsonObject().has("value")) {
      obj.setValue(tamperData.getAsJsonObject().get("value").getAsFloat());
    }

    return obj.toJson();
  }
}
