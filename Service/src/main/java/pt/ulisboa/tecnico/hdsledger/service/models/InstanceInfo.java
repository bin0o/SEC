package pt.ulisboa.tecnico.hdsledger.service.models;


import pt.ulisboa.tecnico.hdsledger.communication.CommitMessage;
import pt.ulisboa.tecnico.hdsledger.communication.ConsensusMessage;

import java.util.Collection;

public class InstanceInfo {

    private int currentRound = 1;
    private int preparedRound = -1;
    private String preparedValue;
    private CommitMessage commitMessage;
    private String inputValue;

    private Integer timer;

    private Collection<ConsensusMessage> preparedJustificationMessage;

    private String clientId;

    private int committedRound = -1;

    public Collection<ConsensusMessage> getPreparedJustificationMessage(){
        return this.preparedJustificationMessage;
    }

    public void setPreparedJustificationMessage(Collection<ConsensusMessage> msg){
        this.preparedJustificationMessage = msg;
    }

    public Integer getTimer() {
        return this.timer;
    }

    public void setTimer(Integer timer){
        this.timer = timer;
    }

    public InstanceInfo(String inputValue, String clientId, Integer timer) {
        this.inputValue = inputValue;
        this.clientId = clientId;
        this.timer = timer;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public void setCurrentRound(int currentRound) {
        this.currentRound = currentRound;
    }

    public int getPreparedRound() {
        return preparedRound;
    }

    public void setPreparedRound(int preparedRound) {
        this.preparedRound = preparedRound;
    }

    public String getPreparedValue() {
        return preparedValue;
    }

    public void setPreparedValue(String preparedValue) {
        this.preparedValue = preparedValue;
    }

    public String getInputValue() {
        return inputValue;
    }

    public void setInputValue(String inputValue) {
        this.inputValue = inputValue;
    }

    public int getCommittedRound() {
        return committedRound;
    }

    public void setCommittedRound(int committedRound) {
        this.committedRound = committedRound;
    }

    public CommitMessage getCommitMessage() {
        return commitMessage;
    }

    public void setCommitMessage(CommitMessage commitMessage) {
        this.commitMessage = commitMessage;
    }
}
