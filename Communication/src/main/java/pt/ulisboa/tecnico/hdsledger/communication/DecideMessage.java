package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import pt.ulisboa.tecnico.hdsledger.common.models.Block;
import pt.ulisboa.tecnico.hdsledger.common.MessageTampering;

public class DecideMessage implements MessageTampering {
  private boolean confirmation;

  private int index;

  private Block value;

  public DecideMessage(boolean confirmation, int index, Block value) {
    this.confirmation = confirmation;
    this.index = index;
    this.value = value;
  }

  public boolean getConfirmation() {
    return this.confirmation;
  }

  public int getIndex() {
    return this.index;
  }

  public Block getValue() {
    return this.value;
  }

  public void setValue(Block value) {
    this.value = value;
  }

  public void setConfirmation(boolean confirmation) {
    this.confirmation = confirmation;
  }

  public void setIndex(int index) {
    this.index = index;
  }

  public String toJson() {
    return new Gson().toJson(this);
  }

  @Override
  public String tamperJson(JsonElement tamperData) {
    Gson gson = new Gson();

    // Create copy
    DecideMessage obj = gson.fromJson(this.toJson(), DecideMessage.class);

    if (tamperData.getAsJsonObject().has("value")) {
      Block block = gson.fromJson(tamperData.getAsJsonObject().get("value"), Block.class);
      obj.setValue(block);
    }

    if (tamperData.getAsJsonObject().has("index"))
      obj.setIndex(tamperData.getAsJsonObject().get("index").getAsInt());

    if (tamperData.getAsJsonObject().has("confirmation"))
      obj.setConfirmation(tamperData.getAsJsonObject().get("confirmation").getAsBoolean());

    return obj.toJson();
  }

  @Override
  public String toString() {
    return "AppendReplyMessage{"
        + "confirmation="
        + confirmation
        + ", index="
        + index
        + ", value="
        + value
        + '}';
  }
}
