package io.vertx.conduit.errors;

public class AuthenticationError extends Error{

  public AuthenticationError(String authenticationErrorMessage) {
    super(authenticationErrorMessage);
  }

  public AuthenticationError(){
    super(ErrorMessages.AUTHENTICATION_ERROR_DEFAULT);
  }

}
