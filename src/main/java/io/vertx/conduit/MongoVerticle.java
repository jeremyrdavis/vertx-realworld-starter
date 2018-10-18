package io.vertx.conduit;

import io.vertx.conduit.users.models.MongoConstants;
import io.vertx.conduit.users.models.User;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.mongo.MongoAuth;
import io.vertx.ext.auth.mongo.impl.DefaultHashStrategy;
import io.vertx.ext.auth.mongo.impl.MongoUser;
import io.vertx.ext.mongo.MongoClient;

public class MongoVerticle extends AbstractVerticle {

  public static final String MESSAGE_ADDRESS = "address.login";
  public static final String MESSAGE_ACTION = "persistence.action";
  public static final String MESSAGE_ACTION_FOLLOW_USER = "action.follow";
  public static final String MESSAGE_ACTION_REGISTER = "action.register";
  public static final String MESSAGE_ACTION_LOGIN = "action.login";
  public static final String MESSSAGE_ACTION_LOOKUP_USER_BY_EMAIL = "persistence.lookup.user.by.email";
  public static final String MESSAGE_ACTION_LOOKUP_USER_BY_EMAIL_VALUE = "email";
  public static final String MESSSAGE_ACTION_LOOKUP_USER_BY_USERNAME = "persistence.lookup.user.by.username";
  public static final String MESSAGE_ACTION_LOOKUP_USER_BY_USERNAME_VALUE = "username";
  public static final String MESSAGE_FOLLOW_USER_FOLLOWED_USER = "followed";
  public static final String MESSAGE_FOLLOW_USER_FOLLOWER = "follower";
  public static final String MESSAGE_RESPONSE = "response";
  public static final String MESSAGE_RESPONSE_SUCCESS = "success";
  public static final String MESSAGE_RESPONSE_FAILURE = "failure";
  public static final String MESSAGE_RESPONSE_DETAILS = "details";
  public static final String MESSAGE_VALUE_USER = "user";
  public static final String MESSAGE_LOOKUP_CRITERIA = "criteria";

  // for DB access
  private MongoClient mongoClient;

  // Authentication provider for logging in
  private MongoAuth loginAuthProvider;

  @Override
  public void start(Future<Void> startFuture) {

    System.out.println(config().getString("db_name"));
    // Configure the MongoClient inline.  This should be externalized into a config file
    mongoClient = MongoClient.createShared(vertx, new JsonObject().put("db_name", config().getString("db_name", "conduit")).put("connection_string", config().getString("connection_string", "mongodb://localhost:27017")));

    // Configure authentication with MongoDB
    loginAuthProvider = MongoAuth.create(mongoClient, new JsonObject());
    loginAuthProvider.setUsernameField("email");

    JsonObject authProperties = new JsonObject();
    MongoAuth authProvider = MongoAuth.create(mongoClient, authProperties);

    EventBus eventBus = vertx.eventBus();
    MessageConsumer<JsonObject> consumer = eventBus.consumer(MESSAGE_ADDRESS);

    consumer.handler(message -> {

      String action = message.body().getString(MESSAGE_ACTION);
      System.out.println(action);

      switch (action) {
        case MESSAGE_ACTION_REGISTER:
          registerUser(message);
          break;
        case MESSAGE_ACTION_LOGIN:
          loginUser(message);
          break;
        case MESSSAGE_ACTION_LOOKUP_USER_BY_EMAIL:
          lookupUserByEmail(message);
          break;
        case MESSSAGE_ACTION_LOOKUP_USER_BY_USERNAME:
          lookupUserByUsername(message);
          break;
        case MESSAGE_ACTION_FOLLOW_USER:
          followUser(message);
          break;
        default:
          message.fail(1, "Unkown action: " + message.body());
      }
    });

    startFuture.complete();
  }

  private void followUser(Message<JsonObject> message) {

    try {
      // Get the user to follow
      String username = message.body().getString(MESSAGE_FOLLOW_USER_FOLLOWED_USER);
      findUserByUsername(username).setHandler(ar -> {

        if (ar.succeeded()) {
          User followed = ar.result();

          // Get the user to update
          findUserByEmail(message.body().getString(MESSAGE_FOLLOW_USER_FOLLOWER)).setHandler(ar2 -> {
            if (ar2.succeeded()) {

              User follower = ar2.result();
              follower.follow(followed);
              addFollower(follower, followed).setHandler(ar3 -> {

                // Update the user
                if (ar3.succeeded()) {
                  message.reply(
                    new JsonObject()
                      .put(MESSAGE_RESPONSE, MESSAGE_RESPONSE_SUCCESS)
                      .put(MESSAGE_RESPONSE_DETAILS, new JsonObject()
                        .put(MESSAGE_FOLLOW_USER_FOLLOWER, follower.toMongoJson())
                        .put(MESSAGE_FOLLOW_USER_FOLLOWED_USER, followed.toMongoJson())));
                }
              });
            } else {
              throw new RuntimeException(ar2.cause());
            }
          });
        } else {
          throw new RuntimeException(ar.cause());
        }
      });
    } catch (Exception e) {

      message.reply(
        new JsonObject()
          .put(MESSAGE_RESPONSE, MESSAGE_RESPONSE_FAILURE)
          .put(MESSAGE_RESPONSE_DETAILS, e.getMessage()));
    }


  }

  private Future<Void> addFollower(User userToUpdate, User followed) {
    Future<Void> retVal = Future.future();

    JsonObject query = new JsonObject().put("email", userToUpdate.getEmail());
    JsonObject update = new JsonObject()
      .put("$set", new JsonObject()
        .put("following", followed.get_id()));

    mongoClient.updateCollection(MongoConstants.COLLECTION_NAME_USERS, query, update, ar -> {
      if (ar.succeeded()) {
        retVal.complete();
      }else{
        retVal.fail(ar.cause());
      }
    });

    return retVal;
  }

  private Future<User> findUserByEmail(String email){
    Future<User> retVal = Future.future();

    JsonObject query = new JsonObject().put("email", email);
    mongoClient.find(MongoConstants.COLLECTION_NAME_USERS, query, ar -> {

      if (ar.succeeded()) {
        User userToFollow = new User(ar.result().get(0));
        retVal.complete(userToFollow);
      } else {
        retVal.fail(ar.cause());
      }
    });
    return retVal;
  }

  private Future<User> findUserByUsername(String username) {

    Future<User> retVal = Future.future();

    JsonObject query = new JsonObject().put("username", username);
    mongoClient.find(MongoConstants.COLLECTION_NAME_USERS, query, ar -> {

      if (ar.succeeded()) {
        User userToFollow = new User(ar.result().get(0));
        retVal.complete(userToFollow);
      } else {
        retVal.fail(ar.cause());
      }
    });
    return retVal;
  }

  private void lookupUserByUsername(Message<JsonObject> message) {
    JsonObject query = new JsonObject()
      .put("username", message.body().getString(MESSAGE_LOOKUP_CRITERIA));

    mongoClient.find(MongoConstants.COLLECTION_NAME_USERS, query, res -> {
      if (res.succeeded()) {
        System.out.println("lookupUserByUsername for username " + message.body().getString(MESSAGE_LOOKUP_CRITERIA) + " result: " + res.result());
        message.reply(new JsonObject()
          .put(MESSAGE_RESPONSE, MESSAGE_RESPONSE_SUCCESS)
          .put(MESSAGE_RESPONSE_DETAILS, res.result().get(0)));
      } else {
        message.reply(new JsonObject()
          .put(MESSAGE_RESPONSE, MESSAGE_RESPONSE_FAILURE)
          .put(MESSAGE_RESPONSE_DETAILS, res.cause().getMessage()));
      }
    });
  }

  private void lookupUserByEmail(Message<JsonObject> message) {

    JsonObject query = new JsonObject()
      .put("email", message.body().getString(MESSAGE_ACTION_LOOKUP_USER_BY_EMAIL_VALUE));

    mongoClient.find(MongoConstants.COLLECTION_NAME_USERS, query, res -> {
      if (res.succeeded()) {
        System.out.println("lookupUserByEmail for email " + message.body().getString(MESSAGE_ACTION_LOOKUP_USER_BY_EMAIL_VALUE) + " result: " + res.result());
        message.reply(new JsonObject()
          .put(MESSAGE_RESPONSE, MESSAGE_RESPONSE_SUCCESS)
          .put(MESSAGE_RESPONSE_DETAILS, res.result().get(0)));
      } else {
        message.reply(new JsonObject()
          .put(MESSAGE_RESPONSE, MESSAGE_RESPONSE_FAILURE)
          .put(MESSAGE_RESPONSE_DETAILS, res.cause().getMessage()));
      }
    });
  }

  private void loginUser(Message<JsonObject> message) {

    //
    JsonObject authInfo = message.body().getJsonObject(MESSAGE_VALUE_USER);

    System.out.println(authInfo.toString());

    loginAuthProvider.authenticate(authInfo, ar -> {
      if (ar.succeeded()) {
        message.reply(new JsonObject()
          .put(MESSAGE_RESPONSE, MESSAGE_RESPONSE_SUCCESS));
      } else {
        message.reply(new JsonObject()
          .put(MESSAGE_RESPONSE, MESSAGE_RESPONSE_FAILURE)
          .put(MESSAGE_RESPONSE_DETAILS, ar.cause().getMessage()));
      }
    });

  }

  private void registerUser(Message<JsonObject> message) {

    final User userToRegister = new User(message.body().getJsonObject(MESSAGE_VALUE_USER));

    insertUser(userToRegister).setHandler(ar -> {

      if (ar.succeeded()) {
        message.reply(new JsonObject()
          .put(MESSAGE_RESPONSE, MESSAGE_RESPONSE_SUCCESS)
          .put(MESSAGE_RESPONSE_DETAILS, userToRegister.toJson()));
      } else {
        message.reply(new JsonObject()
          .put(MESSAGE_RESPONSE, MESSAGE_RESPONSE_FAILURE)
          .put(MESSAGE_RESPONSE_DETAILS, ar.cause()));
      }
    });

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

}
