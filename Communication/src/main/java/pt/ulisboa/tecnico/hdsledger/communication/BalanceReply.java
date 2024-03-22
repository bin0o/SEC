package pt.ulisboa.tecnico.hdsledger.communication;

public class BalanceReply {
  private final int value;

  public BalanceReply(int value) {
    this.value = value;
  }

  public int getValue() {
    return this.value;
  }
}
