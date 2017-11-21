package io.vertx.conduit.users.models;

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

  private String password;

  public User() {
  }

  public User(String username, String email, String password) {
    this.username = username;
    this.email = email;
    this.password = password;
  }


  public User(String username, String email, String bio, String image, String password) {
    this.username = username;
    this.email = email;
    this.bio = bio;
    this.image = image;
    this.password = password;
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

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}
