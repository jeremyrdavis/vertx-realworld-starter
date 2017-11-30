package io.vertx.conduit.users.models;

import io.vertx.core.json.JsonObject;

/**
 * username: String,
 email: String,
 bio: String,
 image: String,
 hash: String,
 salt: String
 */
public class User {

  String _id;

  String username;

  String email;

  String password;

  public User() {
  }

  public User(String email, String password) {
    this.email = email;
    this.password = password;
  }

  public User(String username, String email, String password) {
    this.username = username;
    this.email = email;
    this.password = password;
  }

  public User(JsonObject jsonObject){
    this._id = jsonObject.getString("_id");
    this.username = jsonObject.getString("username");
    this.email = jsonObject.getString("email");

  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject()
      .put("username", username)
      .put("email", email);
    if (_id != null && !_id.isEmpty()) {
      json.put("_id", _id);
    }
    return json;
  }

  public String get_id() {
    return _id;
  }

  public void set_id(String _id) {
    this._id = _id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}
