package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import pt.ulisboa.tecnico.hdsledger.common.MessageTampering;
import pt.ulisboa.tecnico.hdsledger.common.models.Block;

import java.lang.reflect.Type;
import java.util.Collection;

public class PrePrepareMessage implements MessageTampering {

  // Value
  private Block block;
  private String signature;
  private Collection<ConsensusMessage> justification;

  public PrePrepareMessage(Block block) {
    this.block = block;
  }

  public void setSignature(String signature) {
    this.signature = signature;
  }

  public void setJustification(Collection<ConsensusMessage> justification) {
    this.justification = justification;
  }

  public String getSignature() {
    return signature;
  }

  public Block getBlock() {
    return block;
  }

  public void setBlock(Block block) {
    this.block = block;
  }

  public Collection<ConsensusMessage> getJustification() {
    return justification;
  }

  public String toJson() {
    return new Gson().toJson(this);
  }

  @Override
  public String tamperJson(JsonElement tamperData) {
    Gson gson = new Gson();

    // Create copy
    PrePrepareMessage obj = gson.fromJson(this.toJson(), PrePrepareMessage.class);

    if (tamperData.getAsJsonObject().has("block")) {
      Block block = gson.fromJson(tamperData.getAsJsonObject().get("block"), Block.class);
      obj.setBlock(block);
    }

    if (tamperData.getAsJsonObject().has("justification")) {
      Type justificationType = new TypeToken<Collection<ConsensusMessage>>() {}.getType();

      Collection<ConsensusMessage> justification =
          gson.fromJson(tamperData.getAsJsonObject().get("justification"), justificationType);
      obj.setJustification(justification);
    }

    if (tamperData.getAsJsonObject().has("signature")) {
      obj.setSignature(tamperData.getAsJsonObject().get("signature").getAsString());
    }

    return obj.toJson();
  }
}
