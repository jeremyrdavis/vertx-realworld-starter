package io.vertx.conduit;

import io.vertx.core.json.JsonObject;

public interface ConduitDomainModel {

    public JsonObject toJson();

    public JsonObject toConduitJson();

    public JsonObject toMongoJson();
}
