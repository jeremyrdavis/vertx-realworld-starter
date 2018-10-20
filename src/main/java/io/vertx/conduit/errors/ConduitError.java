package io.vertx.conduit.errors;

import io.vertx.core.json.JsonObject;

public class ConduitError {

    String body;

    public JsonObject toConduitJson(){
        return new JsonObject().put("errors", new JsonObject().put("body", body));
    }

    public ConduitError(String body) {
        this.body = body;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
