package pt.ulisboa.tecnico.hdsledger.common;

public enum MessageType {
  APPEND,
  DECIDE,
  PRE_PREPARE,
  PREPARE,
  COMMIT,
  ACK,
  IGNORE,
  ROUND_CHANGE,
  CHECK_BALANCE
}
