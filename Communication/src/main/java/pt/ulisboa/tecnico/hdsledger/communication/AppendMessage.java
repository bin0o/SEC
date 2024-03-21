package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;
import pt.ulisboa.tecnico.hdsledger.service.models.Transaction;

public class AppendMessage {
    private final Transaction value;

    private final String justification;

    public AppendMessage(Transaction value, String justification) {
        this.value = value;
        this.justification = justification;
    }

    public String getJustification() { return justification; }

    public Transaction getValue() {
        return value;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
}
