package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;
import pt.ulisboa.tecnico.hdsledger.common.MessageType;

public class ConsensusMessage extends Message {

  // Consensus instance
  private int consensusInstance;
  // Round
  private int round;
  // Who sent the previous message
  private String replyTo;
  // Id of the previous message
  private int replyToMessageId;
  // Message (PREPREPARE, PREPARE, COMMIT)
  private String message;

  public ConsensusMessage(String senderId, MessageType type) {
    super(senderId, type);
  }

  public AppendMessage deserializeStartMessage() {
    return new Gson().fromJson(this.message, AppendMessage.class);
  }

  public DecideMessage deserializeDecideMessage() {
    return new Gson().fromJson(this.message, DecideMessage.class);
  }

  public BalanceReply deserializeBalanceReply() {
    return new Gson().fromJson(this.message, BalanceReply.class);
  }

  public RoundChangeMessage deserializeRoundChangeMessage() {
    return new Gson().fromJson(this.message, RoundChangeMessage.class);
  }

  public PrePrepareMessage deserializePrePrepareMessage() {
    return new Gson().fromJson(this.message, PrePrepareMessage.class);
  }

  public PrepareMessage deserializePrepareMessage() {
    return new Gson().fromJson(this.message, PrepareMessage.class);
  }

  public CommitMessage deserializeCommitMessage() {
    return new Gson().fromJson(this.message, CommitMessage.class);
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public int getConsensusInstance() {
    return consensusInstance;
  }

  public void setConsensusInstance(int consensusInstance) {
    this.consensusInstance = consensusInstance;
  }

  public int getRound() {
    return round;
  }

  public void setRound(int round) {
    this.round = round;
  }

  public String getReplyTo() {
    return replyTo;
  }

  public void setReplyTo(String replyTo) {
    this.replyTo = replyTo;
  }

  public int getReplyToMessageId() {
    return replyToMessageId;
  }

  public void setReplyToMessageId(int replyToMessageId) {
    this.replyToMessageId = replyToMessageId;
  }
}
