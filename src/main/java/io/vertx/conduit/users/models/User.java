package io.vertx.conduit.users.models;

import java.sql.Timestamp;

/**
 * username: String,
 email: String,
 bio: String,
 image: String,
 hash: String,
 salt: String
 */
public class User {

  private String username;

  private String email;

  private String bio;

  private String image;

  private String hash;

  private String salt;

  public User() {
  }

  public User(String username, String email, String bio, String image, String hash, String salt) {
    this.username = username;
    this.email = email;
    this.bio = bio;
    this.image = image;
    this.hash = hash;
    this.salt = salt;
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

  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  public String getSalt() {
    return salt;
  }

  public void setSalt(String salt) {
    this.salt = salt;
  }
}
