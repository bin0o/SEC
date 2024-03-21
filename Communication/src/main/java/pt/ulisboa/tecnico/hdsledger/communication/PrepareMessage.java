package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;
import pt.ulisboa.tecnico.hdsledger.service.models.Transaction;

import java.util.concurrent.ThreadLocalRandom;

public class PrepareMessage {
    
    // Value
    private Transaction value;

    public PrepareMessage(Transaction value) {
        this.value = value;
    }

    public Transaction getValue() {
        return value;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
}   
