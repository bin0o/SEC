package pt.ulisboa.tecnico.hdsledger.communication;
import com.google.gson.Gson;

public class RoundChangeMessage {
    private final ConsensusMessage justification;
    private final int preparedRound;
    private final String preparedValue;

    public RoundChangeMessage(ConsensusMessage justification, int preparedRound, String preparedValue) {
        this.justification = justification;
        this.preparedRound = preparedRound;
        this.preparedValue = preparedValue;
    }
    public ConsensusMessage getJustification() {
        return justification;
    }

    public int getPreparedRound() {
        return preparedRound;
    }

    public String getPreparedValue() {
        return preparedValue;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
}
