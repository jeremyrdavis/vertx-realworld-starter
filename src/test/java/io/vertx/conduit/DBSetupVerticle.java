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

            dropCollectionUsers().setHandler(ar ->{
                if (ar.succeeded()) {
                    insertJacob().setHandler(ar2 -> {
                        if (ar.succeeded()) {
                            updateJacob().setHandler(ar3 -> {
                                if (ar3.failed()) {
                                    startFuture.fail(ar3.cause());
                                }else{
                                    startFuture.complete();
                                }
                            });
                        }else{
                            startFuture.fail(ar.cause());
                        }
                    });
                }else{
                    startFuture.fail(ar.cause());
                }
            });
        /*
        CompositeFuture.all(dropCollectionUsers(), insertJacob(), updateJacob()).setHandler(ar ->{
            if (ar.succeeded()) {
                System.out.println("collection dropped");
                startFuture.complete();
            } else {
                startFuture.fail(ar.cause());
            }
        });
*/


/*
    // drop the database
    mongoClient.dropCollection(MongoConstants.COLLECTION_NAME_USERS, res ->{
        if (res.succeeded()) {
            mongoClient.createCollection(MongoConstants.COLLECTION_NAME_USERS, r2 ->{
                if (r2.succeeded()) {

                    new JsonObject()
                            .put("email", "jake@jake.jake")
                            .put("username", "Jacob")
                            .put("password", "jakejake");

                    loginAuthProvider.insertUser("Jacob", "jakejake", null, null, ins -> {
                        if (ins.succeeded()) {
                            startFuture.complete();
                        }else{
                            startFuture.fail(ins.cause());
                        }
                    } );
                }else{
                    startFuture.fail(res.cause());
                }
            });
        }else{
            startFuture.fail(res.cause());
        }
    });
*/


/*
    // Configure authentication with MongoDB
    loginAuthProvider = MongoAuth.create(mongoClient, new JsonObject());
    loginAuthProvider.setUsernameField("email");

    JsonObject authProperties = new JsonObject();
    MongoAuth authProvider = MongoAuth.create(mongoClient, authProperties);
*/

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

    private Future<Void> updateJacob(){
        Future<Void> retVal = Future.future();
        JsonObject query = new JsonObject().put("username", "Jacob");
        JsonObject update = new JsonObject().put("$set", new JsonObject().put("email", "jake@jake.jake"));
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
}
