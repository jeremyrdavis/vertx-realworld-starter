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

public class UserDAV extends AbstractVerticle {

    public static final String MESSAGE_ADDRESS = "address.login";

    // actions
    public static final String MESSAGE_ACTION = "action";
    public static final String MESSAGE_ACTION_FOLLOW_USER = "action.follow";
    public static final String MESSAGE_ACTION_LOGIN = "action.login";
    public static final String MESSAGE_ACTION_LOOKUP_USER_BY_EMAIL = "persistence.lookup.user.by.email";
    public static final String MESSAGE_ACTION_LOOKUP_USER_BY_USERNAME = "persistence.lookup.user.by.username";
    public static final String MESSAGE_ACTION_REGISTER = "action.register";
    public static final String MESSAGE_ACTION_UNFOLLOW = "action.unfollow";
    public static final String MESSAGE_ACTION_UPDATE = "action.update";

    public static final String MESSAGE_RESPONSE_DETAILS = "details";

    public static final String MESSAGE_FOLLOW_USER_FOLLOWED_USER = "followed";
    public static final String MESSAGE_FOLLOW_USER_FOLLOWER = "follower";
    public static final String MESSAGE_VALUE_USER = "user";
    public static final String MESSAGE_LOOKUP_CRITERIA = "criteria";
    public static final String MESSAGE_UPDATE_EXISTING = "existing";
    public static final String MESSAGE_UPDATE_NEW = "new";

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
        loginAuthProvider.setUsernameCredentialField("email");

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
                case MESSAGE_ACTION_LOOKUP_USER_BY_EMAIL:
                    lookupUserByEmail(message);
                    break;
                case MESSAGE_ACTION_LOOKUP_USER_BY_USERNAME:
                    lookupUserByUsername(message);
                    break;
                case MESSAGE_ACTION_FOLLOW_USER:
                    followUser(message);
                    break;
                case MESSAGE_ACTION_UPDATE:
                    updateUser(message);
                    break;
                case MESSAGE_ACTION_UNFOLLOW:
                    unfollowUser(message);
                    break;
                default:
                    message.fail(1, "Unkown action: " + message.body());
            }
        });

        startFuture.complete();
    }

    private void unfollowUser(Message<JsonObject> message) {
        // Get the user to follow
        String username = message.body().getString(MESSAGE_FOLLOW_USER_FOLLOWED_USER);
        findUserByUsername(username).setHandler(ar -> {

            if (ar.succeeded()) {
                User followed = ar.result();

                // Get the user to update
                findUserByEmail(message.body().getString(MESSAGE_FOLLOW_USER_FOLLOWER)).setHandler(ar2 -> {
                    if (ar2.succeeded()) {

                        User follower = ar2.result();
                        follower.unFollow(followed);

                        updateUserWithNewValues(follower).setHandler(ar3 -> {

                            // Update the user
                            if (ar3.succeeded()) {
                                message.reply(
                                        new JsonObject()
                                                .put(MESSAGE_RESPONSE_DETAILS, follower.toJson()));
                            }
                        });
                    } else {
                        message.fail(MessagingErrorCodes.LOOKUP_FAILED.ordinal(), MessagingErrorCodes.LOOKUP_FAILED.message);
                    }
                });
            }else {
                message.fail(MessagingErrorCodes.LOOKUP_FAILED.ordinal(), MessagingErrorCodes.LOOKUP_FAILED.message);
            }
        });
    }

    private void followUser(Message<JsonObject> message) {

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
                                                .put(MESSAGE_RESPONSE_DETAILS, new JsonObject()
                                                        .put(MESSAGE_FOLLOW_USER_FOLLOWER, follower.toMongoJson())
                                                        .put(MESSAGE_FOLLOW_USER_FOLLOWED_USER, followed.toMongoJson())));
                            }
                        });
                    }else {
                        message.fail(MessagingErrorCodes.LOOKUP_FAILED.ordinal(), MessagingErrorCodes.LOOKUP_FAILED.message);
                    }
                });
            }else {
                message.fail(MessagingErrorCodes.LOOKUP_FAILED.ordinal(), MessagingErrorCodes.LOOKUP_FAILED.message);
            }
        });

//            message.fail(MessagingErrorCodes.UNKNOWN_ERROR.ordinal(), MessagingErrorCodes.UNKNOWN_ERROR.message);


    }

    private void updateUser(Message<JsonObject> message) {

        JsonObject valuesToUpdate = message.body().getJsonObject(MESSAGE_UPDATE_NEW);
        String username = message.body().getString(MESSAGE_UPDATE_EXISTING);

        JsonObject query = new JsonObject().put("username", username);
        JsonObject update = new JsonObject().put("$set", valuesToUpdate);


        mongoClient.updateCollection(MongoConstants.COLLECTION_NAME_USERS, query, update, ar -> {

            if (ar.succeeded()) {

                mongoClient.find(MongoConstants.COLLECTION_NAME_USERS, new JsonObject().put("username", username), ar2 -> {
                    if (ar2.succeeded()) {
                        JsonObject result = ar2.result().get(0);
                        message.reply(new JsonObject().put(MESSAGE_RESPONSE_DETAILS, result));
                    } else {
                        message.fail(1, ar2.cause().getMessage());
                    }
                });
            } else {
                message.fail(1, ar.cause().getMessage());
            }
        });
    }

    /**
     * @param userToUpdate User object with new values that need to be persisted
     * @return
     */
    private Future<Void> updateUserWithNewValues(User userToUpdate) {
        Future<Void> retVal = Future.future();

        JsonObject newValues = userToUpdate.toMongoJson();
        newValues.remove("_id");
        JsonObject query = new JsonObject().put("_id", userToUpdate.get_id());
        JsonObject update = new JsonObject().put("$set", newValues);

        mongoClient.updateCollection(MongoConstants.COLLECTION_NAME_USERS, query, update, ar -> {
            if (ar.succeeded()) {
                retVal.complete();
            } else {
                retVal.fail(ar.cause());
            }
        });

        return retVal;
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
            } else {
                retVal.fail(ar.cause());
            }
        });

        return retVal;
    }

    private Future<User> findUserByEmail(String email) {
        System.out.println("findUserByEmail: " + email);
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
                System.out.println(ar.result().get(0));
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
                        .put(MESSAGE_RESPONSE_DETAILS, res.result().get(0)));
            } else {
                message.fail(MessagingErrorCodes.NOT_FOUND.ordinal(), MessagingErrorCodes.NOT_FOUND.message + res.cause());
            }
        });
    }

    private void lookupUserByEmail(Message<JsonObject> message) {

        JsonObject query = new JsonObject()
                .put("email", message.body().getString(MESSAGE_LOOKUP_CRITERIA));

        mongoClient.find(MongoConstants.COLLECTION_NAME_USERS, query, res -> {
            if (res.succeeded()) {
                System.out.println("lookupUserByEmail for email " + message.body().getString(MESSAGE_LOOKUP_CRITERIA) + " result: " + res.result());
                message.reply(new JsonObject()
                        .put(MESSAGE_RESPONSE_DETAILS, res.result().get(0)));
            } else {
                message.fail(MessagingErrorCodes.NOT_FOUND.ordinal(), MessagingErrorCodes.NOT_FOUND.message + res.cause());
            }
        });
    }

    private Future<User> lookupUserByCriteria(String criteria, String value) {
        Future<User> retVal = Future.future();

        JsonObject query = new JsonObject()
                .put(criteria, value);
        mongoClient.find(MongoConstants.COLLECTION_NAME_USERS, query, res -> {
            if (res.succeeded()) {
                retVal.complete(new User(res.result().get(0)));
            } else {
                retVal.fail(res.cause());
            }
        });
        return retVal;
    }


    private void loginUser(Message<JsonObject> message) {

        //
        JsonObject authInfo = message.body().getJsonObject(MESSAGE_VALUE_USER);

        System.out.println(authInfo.toString());

        loginAuthProvider.authenticate(authInfo, ar -> {
            if (ar.succeeded()) {

                lookupUserByCriteria("email", authInfo.getString("email")).setHandler(ar2 -> {
                    if (ar2.succeeded()) {
                        System.out.println(ar2.result());
                        message.reply(new JsonObject()
                                .put(MESSAGE_RESPONSE_DETAILS, ar2.result().toJson()));
                    } else {
                        message.reply(new JsonObject()
                                .put(MESSAGE_RESPONSE_DETAILS, ar2.cause().getMessage()));
                    }
                });
            } else {
                message.reply(new JsonObject()
                        .put(MESSAGE_RESPONSE_DETAILS, ar.cause().getMessage()));
            }
        });

    }

    private void registerUser(Message<JsonObject> message) {

        final User userToRegister = new User(message.body().getJsonObject(MESSAGE_VALUE_USER));

        insertUser(userToRegister).setHandler(ar -> {

            if (ar.succeeded()) {
                message.reply(new JsonObject()
                        .put(MESSAGE_RESPONSE_DETAILS, userToRegister.toJson()));
            } else {
                message.fail(MessagingErrorCodes.INSERT_FAILURE.ordinal(), MessagingErrorCodes.INSERT_FAILURE + ar.cause().getMessage());
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
            } else {
                retVal.fail(ar.cause());
            }
        });

        return retVal;
    }

}
