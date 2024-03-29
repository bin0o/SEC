package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import pt.ulisboa.tecnico.hdsledger.common.MessageTampering;
import pt.ulisboa.tecnico.hdsledger.common.models.Block;

public class CommitMessage implements MessageTampering {
  private Block block;

  public CommitMessage(Block block) {
    this.block = block;
  }

  public Block getBlock() {
    return block;
  }

  public void setBlock(Block block) {
    this.block = block;
  }

  public String toJson() {
    return new Gson().toJson(this);
  }

  @Override
  public String tamperJson(JsonElement tamperData) {
    Gson gson = new Gson();

    // Create copy
    CommitMessage obj = gson.fromJson(this.toJson(), CommitMessage.class);

    if (tamperData.getAsJsonObject().has("block")) {
      Block block = gson.fromJson(tamperData.getAsJsonObject().get("block"), Block.class);
      obj.setBlock(block);
    }

    return obj.toJson();
  }
}
