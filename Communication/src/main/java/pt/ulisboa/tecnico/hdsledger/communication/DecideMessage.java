package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;
import pt.ulisboa.tecnico.hdsledger.service.models.Transaction;

public class DecideMessage {

    private final boolean confirmation;

    private final int index;

    private final Transaction value;

    public DecideMessage(boolean confirmation, int index, Transaction value) {
        this.confirmation = confirmation;
        this.index = index;
        this.value = value;
    }

    public boolean getConfirmation() {
        return this.confirmation;
    }

    public int getIndex() {
        return this.index;
    }

    public Transaction getValue() {
        return this.value;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }

    @Override
    public String toString() {
        return "AppendReplyMessage{" +
                "confirmation=" + confirmation +
                ", index=" + index +
                ", value=" + value +
                '}';
    }
}
