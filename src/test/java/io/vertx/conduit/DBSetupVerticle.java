package io.vertx.conduit;

import io.vertx.conduit.MainVerticle;
import io.vertx.conduit.users.models.MongoConstants;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
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

  // MongoDB Collection key for users
  public static final String USER_COLLECTION = "users";

  @Override
  public void start(Future<Void> startFuture) {

    // Configure the MongoClient inline.  This should be externalized into a config file
    mongoClient = MongoClient.createShared(vertx, new JsonObject().put("db_name", DB_NAME_TEST).put("connection_string", DB_CONNECTION_STRING_TEST));

    dropCollectionUsers().setHandler(ar -> {
        if (ar.succeeded()) {
            System.out.println("collection dropped");
            startFuture.complete();
        }else{
            startFuture.fail(ar.cause());
        }
    });


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

  Future<Void> dropCollectionUsers(){
      Future<Void> retVal = Future.future();
      mongoClient.dropCollection(MongoConstants.COLLECTION_NAME_USERS, res ->{
          if (res.succeeded()) {
              retVal.complete();
          }else{
              retVal.fail(res.cause());
          }
      });
      return retVal;
  }

  private Future<Void> insertJacob() {
    Future<Void> future = Future.future();

      JsonObject user = new JsonObject()
              .put("email", "jake@jake.jake")
              .put("username", "Jacob")
              .put("password", "jakejake");

        mongoClient.insert(MongoConstants.COLLECTION_NAME_USERS, user, r -> {

        });

    return future;
  }
}
