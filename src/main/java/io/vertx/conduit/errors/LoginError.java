package io.vertx.conduit.errors;

import io.vertx.core.json.JsonObject;

import java.io.Serializable;

public class LoginError extends Exception implements Serializable{

  public LoginError(String message) {
    super(message);
  }

  public JsonObject toJson() {
    JsonObject message = new JsonObject().put("message", getMessage());
    return new JsonObject().put("message", message);
  }
}
