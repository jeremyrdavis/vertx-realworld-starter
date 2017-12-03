package io.vertx.conduit.errors;

/**
 *
 * Convenience class for storing error messages
 *
 */
public enum ErrorMessages {

  LOGIN_ERROR("No user found with that email and password combination");

  public String message;

  private ErrorMessages(String messageToSet) {
    message = messageToSet;
  }
}
