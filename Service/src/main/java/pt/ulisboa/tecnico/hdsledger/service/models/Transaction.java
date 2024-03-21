package pt.ulisboa.tecnico.hdsledger.service.models;

import java.security.PublicKey;
import java.util.Arrays;

public class Transaction {

    private final PublicKey source;

    private final PublicKey destination;

    private final Integer amount;

    private byte[] sourceSignature;

    private byte[] destinationSignature;

    public Transaction(PublicKey source, PublicKey destination, Integer amount) {
        this.source = source;
        this.destination = destination;
        this.amount = amount;
    }

    public PublicKey getDestination() {
        return destination;
    }

    public void signAsSource(byte[] sourceSignature){
        this.sourceSignature = sourceSignature;
    }

    public void signAsDestination(byte[] destinationSignature){
        this.destinationSignature = destinationSignature;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "source=" + source +
                ", destination=" + destination +
                ", amount=" + amount +
                ", sourceSignature=" + Arrays.toString(sourceSignature) +
                ", destinationSignature=" + Arrays.toString(destinationSignature) +
                '}';
    }
}
