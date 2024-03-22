package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;
import pt.ulisboa.tecnico.hdsledger.common.models.Block;
import pt.ulisboa.tecnico.hdsledger.common.models.Transaction;

public class CommitMessage {
  private final Block block;

  public CommitMessage(Block block) {
    this.block = block;
  }

  public Block getBlock() {
    return block;
  }

  public String toJson() {
    return new Gson().toJson(this);
  }
}
