package io.vertx.conduit;

import io.vertx.conduit.users.models.MongoConstants;
import io.vertx.conduit.users.models.User;
import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.mongo.HashSaltStyle;
import io.vertx.ext.auth.mongo.MongoAuth;
import io.vertx.ext.auth.mongo.impl.DefaultHashStrategy;
import io.vertx.ext.auth.mongo.impl.MongoUser;
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
                .compose(v -> insertUser(new User(null,"Jacob","jake@jake.jake", "jakejake", null, null, "I work at state farm", null)))
                .compose(v -> insertUser(new User(null,"User1","user1@user.user", "user1user1", null, null, "I am User1", null)));
        init.setHandler(startFuture.completer());

    }

    private Future<Void> updateJacob(User user){
        Future<Void> retVal = Future.future();
        JsonObject query = new JsonObject().put("username", user.getUsername());
        JsonObject update = new JsonObject()
                .put("$set", new JsonObject()
                        .put("email", user.getEmail())
                        .put("bio", user.getBio())
                        .put("image", user.getImage()));
        mongoClient.updateCollection(MongoConstants.COLLECTION_NAME_USERS, query, update, res -> {
            if (res.succeeded()) {
                retVal.complete();
            } else {
                retVal.fail(res.cause());
            }
        });
        return retVal;
    }

    private Future<Void> insertUser(User user) {
        Future<Void> retVal = Future.future();

        user.setSalt(DefaultHashStrategy.generateSalt());
        String hashedPassword = loginAuthProvider
                .getHashStrategy().computeHash(user.getPassword(),
                    new MongoUser(
                        new JsonObject()
                            .put("email", user.getEmail()),
                        loginAuthProvider));
        user.setPassword(hashedPassword);

        mongoClient.save(MongoConstants.COLLECTION_NAME_USERS, user.toMongoJson(), ar -> {
            if (ar.succeeded()) {
                user.set_id(ar.result());
                System.out.println("insert successful:" + user.toMongoJson());
                retVal.complete();
            }else{
                retVal.fail(ar.cause());
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
