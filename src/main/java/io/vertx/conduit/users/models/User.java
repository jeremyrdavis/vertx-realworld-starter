package io.vertx.conduit.users.models;

import io.vertx.core.json.JsonObject;

import java.net.URI;

/**
 *
 * username: String,
 * email: String,
 * password: String,
 * token: String
 */
public class User {

  String _id;

  String username;

  String email;

  String password;

  String token;

  String bio;

  String image;

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

  public User(String username, String email, String password, String token) {
    this.username = username;
    this.email = email;
    this.password = password;
    this.token = token;
  }

  public User(JsonObject jsonObject){

    if(jsonObject.containsKey("_id")) this._id = jsonObject.getString("_id");
    this.username = jsonObject.getString("username");
    this.email = jsonObject.getString("email");
    this.password = jsonObject.getString("password");
    if(jsonObject.containsKey("token")) this.token = jsonObject.getString("token");

  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject()
      .put("username", username)
      .put("email", email)
      .put("_id", _id)
      .put("token", token);
    JsonObject retVal = new JsonObject();
    retVal.put("user", json);
    return retVal;
  }

  public JsonObject toMongoJson(){
    return new JsonObject()
      .put("username", username)
      .put("email", email)
      .put("_id", _id)
      .put("token", token)
      .put("bio", bio);

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

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public String getBio() {
    return bio;
  }

  public void setBio(String bio) {
    this.bio = bio;
  }

  public String getImage() {
    return image;
  }

  public void setImage(String image) {
    this.image = image;
  }
}
