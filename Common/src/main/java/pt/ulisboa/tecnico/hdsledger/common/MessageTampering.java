package pt.ulisboa.tecnico.hdsledger.common;

import com.google.gson.JsonElement;

public interface MessageTampering {
  public String tamperJson(JsonElement tamperData);
}
