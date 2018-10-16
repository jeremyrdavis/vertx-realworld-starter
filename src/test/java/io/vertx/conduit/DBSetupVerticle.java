package io.vertx.conduit;

import io.vertx.conduit.users.models.MongoConstants;
import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.mongo.HashSaltStyle;
import io.vertx.ext.auth.mongo.MongoAuth;
import io.vertx.ext.mongo.MongoClient;

import static io.vertx.conduit.TestProps.DB_CONNECTION_STRING_TEST;
import static io.vertx.conduit.TestProps.DB_NAME_TEST;

public class DBSetupVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);

    // for DB access
    private MongoClient mongoClient;

    // Authentication provider for logging in
    private MongoAuth loginAuthProvider;

    @Override
    public void start(Future<Void> startFuture) {

        // Configure the MongoClient inline.  This should be externalized into a config file
        mongoClient = MongoClient.createShared(vertx, new JsonObject().put("db_name", DB_NAME_TEST).put("connection_string", DB_CONNECTION_STRING_TEST));

        JsonObject authConfig = new JsonObject();
        authConfig.put(MongoAuth.PROPERTY_COLLECTION_NAME, MongoAuth.DEFAULT_COLLECTION_NAME);
        authConfig.put(MongoAuth.PROPERTY_SALT_STYLE, HashSaltStyle.COLUMN);;
        loginAuthProvider = MongoAuth.create(mongoClient, authConfig);

        Future<Void> init = dropCollectionUsers()
                .compose(v -> insertJacob())
                .compose(v -> updateJacob());
        init.setHandler(startFuture.completer());
    }

    private Future<Void> updateJacob(){
        Future<Void> retVal = Future.future();
        JsonObject query = new JsonObject().put("username", "Jacob");
        JsonObject update = new JsonObject()
                .put("$set", new JsonObject().put("email", "jake@jake.jake").put("bio", "I work at state farm").put("image", ""));
        mongoClient.updateCollection(MongoConstants.COLLECTION_NAME_USERS, query, update, res -> {
            if (res.succeeded()) {
                retVal.complete();
            } else {
                retVal.fail(res.cause());
            }
        });
        return retVal;
    }

    /*
        Insert the supplied user and return the id
     */
    private Future<String> insertJacob() {
        Future<String> retVal = Future.future();

        JsonObject user = new JsonObject()
                .put("email", "jake@jake.jake")
                .put("username", "Jacob")
                .put("password", "jakejake");

        loginAuthProvider.insertUser("Jacob", "jakejake", null, null, ar -> {
            if (ar.succeeded()) {
                retVal.complete(ar.result());
            }else{
                retVal.fail(ar.cause());
            }
        });

        return retVal;
    }

    Future<Void> dropCollectionUsers() {
        Future<Void> retVal = Future.future();
        mongoClient.dropCollection(MongoConstants.COLLECTION_NAME_USERS, res -> {
            if (res.succeeded()) {
                retVal.complete();
            } else {
                retVal.fail(res.cause());
            }
        });
        return retVal;
    }


}
