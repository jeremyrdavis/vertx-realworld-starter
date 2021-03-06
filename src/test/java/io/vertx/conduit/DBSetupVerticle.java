package io.vertx.conduit;

import io.vertx.conduit.users.models.MongoConstants;
import io.vertx.conduit.users.models.User;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.mongo.HashSaltStyle;
import io.vertx.ext.auth.mongo.MongoAuth;
import io.vertx.ext.mongo.MongoClient;

import java.util.ArrayList;
import java.util.Date;

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
        loginAuthProvider.setUsernameCredentialField("email");
        loginAuthProvider.setUsernameField("email");

        User jacob = new User(null,"Jacob","jake@jake.jake", "jakejake", null, null, "I work at state farm", null);
        User user1 = new User(null,"User1","user1@user.user", "user1user1", null, null, "I am User1", null);
        Date time = new Date();
        Article article = new Article("Test Article 1", "Test description 1", "Lorem ipsum dolor site amet.", new ArrayList<String>(3){ { add("test1"); add("test2"); add("test3"); } });
        article.setCreatedAt(time);
        article.setUpdatedAt(time);
        article.setAuthor(user1);
        article.setSlug("test-article-1");
        Future<Void> init = dropCollection(MongoConstants.COLLECTION_NAME_USERS)
                .compose(v -> dropCollection(MongoConstants.COLLECTION_NAME_ARTICLES))
                .compose(v -> insertUser(jacob))
                .compose(v -> insertUser(user1))
                .compose(v -> insertArticle(article));
        init.setHandler(startFuture.completer());

    }

    private Future<Void> insertArticle(Article article) {
        Future<Void> retVal = Future.future();
        mongoClient.save(MongoConstants.COLLECTION_NAME_ARTICLES, article.toJson(), ar -> {
            if (ar.succeeded()) {
                retVal.complete();
            } else{
                retVal.fail(ar.cause());
            }
        });
        return retVal;
    }
    /*
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
    */
    private Future<Void> insertUser(User user) {
        Future<Void> retVal = Future.future();

        loginAuthProvider.insertUser(user.getEmail(), user.getPassword(), null, null, u ->{

            if (u.succeeded()) {
                System.out.println(u.result());
                JsonObject query = new JsonObject().put("email", user.getEmail());
                JsonObject update = new JsonObject().put("$set",
                        new JsonObject()
                            .put("bio", user.getBio())
                            .put("username", user.getUsername()));
                mongoClient.updateCollection(MongoConstants.COLLECTION_NAME_USERS, query, update, ar -> {
                    if (ar.succeeded()) {
                        System.out.println("insert successful:" + user.toMongoJson());
                        retVal.complete();
                    }else{
                        retVal.fail(ar.cause());
                    }
                });

            }else{
                retVal.fail(u.cause());
            }
        });

        return retVal;
    }
    Future<Void> dropCollection(String collectionName) {
        Future<Void> retVal = Future.future();
        mongoClient.dropCollection(collectionName, res -> {
            if (res.succeeded()) {
                retVal.complete();
            } else {
                retVal.fail(res.cause());
            }
        });
        return retVal;
    }


}
