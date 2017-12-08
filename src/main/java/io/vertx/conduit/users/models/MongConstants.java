package io.vertx.conduit.users.models;

/**
 * Convenience class for db values
 */
public enum MongConstants {

  DB_NAME("conduit"), COLLECTION_NAME_USERS("users");

  public String value;


  private MongConstants(String valueToSet) {
    this.value = valueToSet;
  }
}
