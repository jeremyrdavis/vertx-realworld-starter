package io.vertx.conduit.users.models;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
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

  String salt;

  String token;

  String bio;

  String image;

  List<User> following;

  public void follow(User userToFollow) {
    if (this.following == null) {
      this.following = new ArrayList<>();
    }
    this.following.add(userToFollow);
  }

  public boolean isFollowing(String username) {
    if (following == null) return false;
    boolean isFollowing = false;
    for (User u : following) {
      if(u.getUsername().equalsIgnoreCase(username)){
        isFollowing = true;
      }
    }
    return isFollowing;
  }

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

  public User(String _id, String username, String email, String password, String salt, String token, String bio, String image) {
    this._id = _id;
    this.username = username;
    this.email = email;
    this.password = password;
    this.salt = salt;
    this.token = token;
    this.bio = bio;
    this.image = image;
  }

  public User(JsonObject jsonObject) {

    if (jsonObject.containsKey("_id")) this._id = jsonObject.getString("_id");
    this.username = jsonObject.getString("username");
    this.email = jsonObject.getString("email");
    this.password = jsonObject.getString("password");
    if (jsonObject.containsKey("token")) this.token = jsonObject.getString("token");
    if (jsonObject.containsKey("image")) this.image = jsonObject.getString("image");
    if (jsonObject.containsKey("bio")) this.bio = jsonObject.getString("bio");

  }

  public JsonObject toConduitJson() {
    return new JsonObject()
            .put("user", new JsonObject()
                    .put("email", email)
                    .put("token", token)
                    .put("username", username)
                    .put("bio", bio)
                    .put("image", image));
  }

  public JsonObject toJson() {
    return new JsonObject()
            .put("email", email)
            .put("token", token)
            .put("username", username)
            .put("bio", bio)
            .put("image", image);
  }

  public JsonObject toProfileJson() {
    return new JsonObject()
            .put("profile", new JsonObject()
                    .put("username", username)
                    .put("bio", bio)
                    .put("image", image));
  }

  public JsonObject toMongoJson() {
    JsonObject retVal = new JsonObject();
      if (this._id != null) {
        retVal.put("_id", this._id);
      }
      retVal.put("username", username)
            .put("email", email)
            .put("token", token)
            .put("bio", bio)
            .put("password", this.password)
            .put("salt", this.salt);
    if (this.following != null) {
      JsonArray followedUsers = new JsonArray();
      for(User u : following){
        followedUsers.add(u.get_id());
      }
      retVal.put("following", followedUsers);
    }
    return retVal;
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

  public String getImage() { return image; }

  public void setImage(String image) {
    this.image = image;
  }

  public List<User> getFollowing() {
    return following;
  }

  public void setFollowing(List<User> following) {
    this.following = following;
  }

  public String getSalt() {
    return salt;
  }

  public void setSalt(String salt) {
    this.salt = salt;
  }
}
