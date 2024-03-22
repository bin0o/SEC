package pt.ulisboa.tecnico.hdsledger.common.tests;

import com.google.gson.JsonElement;
import pt.ulisboa.tecnico.hdsledger.common.MessageType;

import java.util.Map;

public class State {
  private MessageType[] DROP;
  private Map<MessageType, JsonElement> TAMPER;

  public MessageType[] getDROP() {
    return DROP;
  }

  public Map<MessageType, JsonElement> getTAMPER() {
    return TAMPER;
  }
}
