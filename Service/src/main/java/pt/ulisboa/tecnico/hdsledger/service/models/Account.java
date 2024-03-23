package pt.ulisboa.tecnico.hdsledger.service.models;

import pt.ulisboa.tecnico.hdsledger.common.ProcessConfig;

import java.security.PublicKey;

public class Account {
  private final ProcessConfig client;
  private Integer balance;

  public Account(ProcessConfig client) {
    this.balance = 100;
    this.client = client;
  }

  public void updateBalance(Integer value) {
    this.balance += value;
  }

  public ProcessConfig getClient() {
    return client;
  }

  public Integer getBalance() {
    return balance;
  }

  public PublicKey getClientPublicKey() {
    return this.client.getPublicKey();
  }
}
