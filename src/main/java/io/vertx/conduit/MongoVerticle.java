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
import io.vertx.ext.mongo.MongoClient;

public class MongoVerticle extends AbstractVerticle{

  public static final String MESSAGE_ADDRESS = "address.login";
  public static final String MESSAGE_ACTION = "persistence.action";
  public static final String MESSAGE_ACTION_REGISTER = "action.register";
  public static final String MESSAGE_ACTION_LOGIN = "action.login";
  public static final String MESSSAGE_ACTION_LOOKUP_USER_BY_EMAIL = "persistence.lookup.user.by.email";
  public static final String MESSAGE_ACTION_LOOKUP_USER_BY_EMAIL_VALUE = "email";
  public static final String MESSAGE_RESPONSE = "response";
  public static final String MESSAGE_RESPONSE_SUCCESS = "success";
  public static final String MESSAGE_RESPONSE_FAILURE = "failure";
  public static final String MESSAGE_RESPONSE_DETAILS = "details";
  public static final String MESSAGE_VALUE_USER = "user";

  // for DB access
  private MongoClient mongoClient;

  // Authentication provider for logging in
  private MongoAuth loginAuthProvider;

  @Override
  public void start(Future<Void> startFuture) {

    System.out.println(config().getString("db_name"));
    // Configure the MongoClient inline.  This should be externalized into a config file
    mongoClient = MongoClient.createShared(vertx, new JsonObject().put("db_name", config().getString("db_name", "conduit")).put("connection_string", config().getString( "connection_string", "mongodb://localhost:27017")));

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
        default:
          message.fail(1, "Unkown action: " + message.body());
      }
    });

    startFuture.complete();
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
      } else{
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

    loginAuthProvider.authenticate(authInfo, ar ->{
      if (ar.succeeded()) {
        message.reply(new JsonObject()
          .put(MESSAGE_RESPONSE, MESSAGE_RESPONSE_SUCCESS));
      }else {
        message.reply(new JsonObject()
          .put(MESSAGE_RESPONSE, MESSAGE_RESPONSE_FAILURE)
          .put(MESSAGE_RESPONSE_DETAILS, ar.cause().getMessage()));
      }
    });

  }

  private void registerUser(Message<JsonObject> message) {

    final User userToRegister = new User(message.body().getJsonObject(MESSAGE_VALUE_USER));

    registerUser(userToRegister).setHandler(ar -> {

      if (ar.succeeded()) {
        message.reply(new JsonObject()
          .put(MESSAGE_RESPONSE, MESSAGE_RESPONSE_FAILURE)
          .put(MESSAGE_RESPONSE_DETAILS, userToRegister.toJson()));
      }else{
        message.reply(new JsonObject()
          .put(MESSAGE_RESPONSE, MESSAGE_RESPONSE_FAILURE)
          .put(MESSAGE_RESPONSE_DETAILS, ar.cause()));
      }
    });

  }

  private Future<User> registerUser(User user) {

    Future<User> retVal = Future.future();

    System.out.println("inserting user: " + user.toConduitJson().toString());

    loginAuthProvider.insertUser(user.getEmail(), user.getPassword(), null, null, ar -> {
      if (ar.succeeded()) {
        // the rest of the User
        JsonObject query = new JsonObject().put("email", user.getEmail());
        JsonObject update = new JsonObject()
          .put("$set", new JsonObject().put("username", user.getUsername()).put("bio", user.getBio()).put("image", user.getImage()));
        mongoClient.updateCollection(MongoConstants.COLLECTION_NAME_USERS, query, update, res -> {
          if (res.succeeded()) {
            System.out.println("updated user");
            retVal.complete(user);
          } else {
            System.out.println("railed to update user");
            retVal.fail(res.cause());
          }
        });
      }else{
        retVal.fail(ar.cause());
      }
    });

    return retVal;
  }


}
