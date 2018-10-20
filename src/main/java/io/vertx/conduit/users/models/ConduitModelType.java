package io.vertx.conduit.users.models;

public enum ConduitModelType {

    USER("user"), ARTICLE("article");

    public String name;

    private ConduitModelType(String nameToSet) {
        this.name = name;
    }

}
