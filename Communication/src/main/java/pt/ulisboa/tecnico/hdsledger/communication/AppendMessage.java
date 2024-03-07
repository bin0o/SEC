package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;

public class AppendMessage {
    private final String value;

    private final String justification;

    public AppendMessage(String value, String justification) {
        this.value = value;
        this.justification = justification;
    }

    public String getJustification() { return justification; }

    public String getValue() {
        return value;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
}
