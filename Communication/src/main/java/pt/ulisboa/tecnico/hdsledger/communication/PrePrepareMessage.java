package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;

import java.util.Collection;
import java.util.List;

public class PrePrepareMessage {
    
    // Value
    private String value;
    private String signature;
    private Collection<ConsensusMessage> justification;

    public PrePrepareMessage(String value) {
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
    public String getValue() {
        return value;
    }

    public Collection<ConsensusMessage> getJustification() {
        return justification;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
}   
