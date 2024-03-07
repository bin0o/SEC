package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;

public class PrePrepareMessage {
    
    // Value
    private String value;

    private String signature;
    public PrePrepareMessage(String value) {
        this.value = value;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getSignature() {
        return signature;
    }
    public String getValue() {
        return value;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
}   
