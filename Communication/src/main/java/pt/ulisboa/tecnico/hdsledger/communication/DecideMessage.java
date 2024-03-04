package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;

public class DecideMessage {

    private final boolean confirmation;

    private final int index;

    public DecideMessage(boolean confirmation, int index) {
        this.confirmation = confirmation;
        this.index = index;
    }

    public boolean getConfirmation() {
        return this.confirmation;
    }

    public int getIndex() {
        return this.index;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }

    @Override
    public String toString() {
        return "AppendReplyMessage{" +
                "confirmation=" + confirmation +
                ", index=" + index +
                '}';
    }
}
