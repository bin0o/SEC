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

  public boolean updateBalance(Integer value) {
    if (this.balance + value > 0 || value > 0) {
      this.balance += value;
      return true;
    }
    return false;
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
