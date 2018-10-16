package io.vertx.starter;

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
    message.reply(new JsonObject()
      .put("email", "email@domain.com")
      .put("username", "user1")
      .put("bio", "i am a user")
      .put("image", ""));
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
    message.reply(new JsonObject()
      .put(MESSAGE_RESPONSE, MESSAGE_RESPONSE_FAILURE)
      .put(MESSAGE_RESPONSE_DETAILS, "Unimplemented"));
  }

}
