package pt.ulisboa.tecnico.hdsledger.communication;
import com.google.gson.Gson;
import pt.ulisboa.tecnico.hdsledger.service.models.Transaction;

import java.util.Collection;
import java.util.List;

public class RoundChangeMessage {
    private final Collection<ConsensusMessage> justification;
    private final int preparedRound;
    private final Transaction preparedValue;

    public RoundChangeMessage(Collection<ConsensusMessage> justification, int preparedRound, Transaction preparedValue) {
        this.justification = justification;
        this.preparedRound = preparedRound;
        this.preparedValue = preparedValue;
    }
    public Collection<ConsensusMessage> getJustification() {
        return justification;
    }

    public int getPreparedRound() {
        return preparedRound;
    }

    public Transaction getPreparedValue() {
        return preparedValue;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
}
