package io.vertx.conduit;

public enum MessagingErrorCodes {

    CONNECTION_ERROR("Database Connection Error: "),
    INSERT_FAILURE("Insert Failed: "),
    LOOKUP_FAILED("Lookup Failed: "),
    NOT_FOUND("Not found: "),
    INVALID_ARGUMENT("Invalid Argument: "),
    UPDATE_FAILURE("Update Failure: "),
    UNKNOWN_ERROR("Undetermined Error: ");

    private MessagingErrorCodes(String msg){
        this.message = msg;
    }

    public final String message;

}
