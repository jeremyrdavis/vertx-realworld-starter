package io.vertx.conduit;

public enum MessagingErrorCodes {

    DB_CONNECTION_ERROR("Database Connection Error: "),
    DB_INSERT_FAILURE("Insert Failed: "),
    NOT_FOUND("Not found: "),
    UNKNOWN_ERROR("Undetermined Error: ");


    public final String message;

    private MessagingErrorCodes(String msg){
        this.message = msg;
    }

}
