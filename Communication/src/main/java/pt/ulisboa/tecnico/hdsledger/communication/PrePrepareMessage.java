package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;
import pt.ulisboa.tecnico.hdsledger.common.models.Block;
import pt.ulisboa.tecnico.hdsledger.common.models.Transaction;

import java.util.Collection;

public class PrePrepareMessage {

  // Value
  private final Block block;
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

  public Collection<ConsensusMessage> getJustification() {
    return justification;
  }

  public String toJson() {
    return new Gson().toJson(this);
  }
}
