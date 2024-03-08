package pt.ulisboa.tecnico.hdsledger.utilities.tests;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import pt.ulisboa.tecnico.hdsledger.utilities.MessageType;

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
