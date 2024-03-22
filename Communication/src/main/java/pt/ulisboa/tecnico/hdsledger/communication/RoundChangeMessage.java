package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;
import pt.ulisboa.tecnico.hdsledger.common.models.Block;
import pt.ulisboa.tecnico.hdsledger.common.models.Transaction;

import java.util.Collection;

public class RoundChangeMessage {
  private final Collection<ConsensusMessage> justification;
  private final int preparedRound;
  private final Block preparedBlock;

  public RoundChangeMessage(
      Collection<ConsensusMessage> justification, int preparedRound, Block preparedBlock) {
    this.justification = justification;
    this.preparedRound = preparedRound;
    this.preparedBlock = preparedBlock;
  }

  public Collection<ConsensusMessage> getJustification() {
    return justification;
  }

  public int getPreparedRound() {
    return preparedRound;
  }

  public Block getPreparedValue() {
    return preparedBlock;
  }

  public String toJson() {
    return new Gson().toJson(this);
  }
}
