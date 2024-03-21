package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;
import pt.ulisboa.tecnico.hdsledger.service.models.Transaction;

import java.util.Collection;
import java.util.List;

public class PrePrepareMessage {
    
    // Value
    private Transaction value;
    private String signature;
    private Collection<ConsensusMessage> justification;

    public PrePrepareMessage(Transaction value) {
        this.value = value;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public void setJustification(Collection<ConsensusMessage> justification) {
        this.justification = justification;
    }

    public String getSignature() {
        return signature;
    }
    public Transaction getValue() {
        return value;
    }

    public Collection<ConsensusMessage> getJustification() {
        return justification;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
}   
