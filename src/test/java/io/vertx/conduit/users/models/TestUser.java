package io.vertx.conduit.users.models;

import io.vertx.core.json.JsonObject;

public class TestUser extends User {

  public TestUser(String email, String password) {
    super(email, password);
  }

  public JsonObject toLoginJson() {
    JsonObject json = new JsonObject()
      .put("email", email)
      .put("password", password);
      return json;
  }
}
