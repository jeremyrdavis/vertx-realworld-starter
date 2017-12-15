package io.vertx.conduit.errors;

import io.vertx.core.json.JsonObject;

public class RegistrationError extends Error{

  public RegistrationError(String message) {
    super(message);
  }

  public RegistrationError(){super(ErrorMessages.REGISTRATION_ERROR);}

  public JsonObject toJson() {
    JsonObject message = new JsonObject().put("message", getMessage());
    return new JsonObject().put("message", message);
  }

}
