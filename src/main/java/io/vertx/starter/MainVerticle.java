package io.vertx.starter;

import io.vertx.conduit.errors.AuthenticationError;
import io.vertx.conduit.errors.ErrorMessages;
import io.vertx.conduit.errors.LoginError;
import io.vertx.conduit.errors.RegistrationError;
import io.vertx.conduit.users.models.MongoConstants;
import io.vertx.conduit.users.models.User;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTOptions;
import io.vertx.ext.auth.mongo.MongoAuth;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.JWTAuthHandler;

public class MainVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);

  // for DB access
  private MongoClient mongoClient;

  // Authentication provider for logging in
  private MongoAuth loginAuthProvider;

  // Authentication provider for the api
  private JWTAuth jwtAuth;

  @Override
  public void start(Future<Void> future) {

    // Configure the MongoClient inline.  This should be externalized into a config file
    mongoClient = MongoClient.createShared(vertx, new JsonObject().put("db_name", "conduit").put("connection_string", "mongodb://localhost:12345"));

    // Configure authentication with MongoDB
    loginAuthProvider = MongoAuth.create(mongoClient, new JsonObject());
    loginAuthProvider.setUsernameField("email");
    JsonObject authProperties = new JsonObject();
    MongoAuth authProvider = MongoAuth.create(mongoClient, authProperties);

    // Configure authentication with JWT
    jwtAuth = JWTAuth.create(vertx, new JsonObject().put("keyStore", new JsonObject()
      .put("type", "jceks")
      .put("path", "keystore.jceks")
      .put("password", "secret")));

    // create a apiRouter to handle the API
    Router baseRouter = Router.router(vertx);
    Router apiRouter = Router.router(vertx);

    baseRouter.route("/").handler(routingContext -> {
      HttpServerResponse response = routingContext.response();
      response.putHeader("content-type", "text/plain").end("Hello Vert.x!");
    });

    apiRouter.route("/user*").handler(BodyHandler.create());
    apiRouter.post("/users").handler( this::registerUser);
    apiRouter.post("/users/login").handler(this::loginUser);

    apiRouter.route().handler(JWTAuthHandler.create(jwtAuth, "/api/users/login"));

    // following routes will be protected
    apiRouter.get("/user").handler(this::getCurrentUser);
    apiRouter.post("/user").handler(this::updateUser);
    baseRouter.mountSubRouter("/api", apiRouter);

    vertx.createHttpServer(new HttpServerOptions()
      .setSsl(true)
      .setKeyStoreOptions(new JksOptions().setPath("server-keystore.jks").setPassword("secret")))
      .requestHandler(baseRouter::accept)
      .listen(8080, result -> {
        if (result.succeeded()) {
          future.complete();
        }else {
          future.fail(result.cause());
        }
      });
  }

  private void updateUser(RoutingContext routingContext) {

    JsonObject body = routingContext.getBodyAsJson();
    final User userToUpdate = Json.decodeValue(routingContext.getBodyAsJson().getJsonObject("user").toString(), User.class);
    routingContext.response()
      .setStatusCode(200)
      .putHeader("Content-Type", "application/json; charset=utf-8")
      //.putHeader("Content-Length", String.valueOf(userResult.toString().length()))
      .end(Json.encodePrettily(userToUpdate.toJson()));

  }

  private void loginUser(RoutingContext routingContext) {

    JsonObject body = routingContext.getBodyAsJson();
//    final User user = new User(routingContext.getBodyAsJson().getJsonObject("user"));

    final User user = Json.decodeValue(routingContext.getBodyAsJson().getJsonObject("user").toString(), User.class);

    JsonObject authInfo = new JsonObject().put("email", user.getEmail()).put("password", user.getPassword());

    loginAuthProvider.setUsernameCredentialField("email");
    loginAuthProvider.setPasswordCredentialField("password");

    loginAuthProvider.authenticate(authInfo, res ->{
      if (res.succeeded()) {
        // lookup the User
              mongoClient.findOne(MongoConstants.COLLECTION_NAME_USERS, new JsonObject().put("email", user.getEmail()),null, r->{
                if(r.succeeded()){
                  User loggedInUser = new User(r.result());
                  // get the JWT Token
                  loggedInUser.setToken(jwtAuth.generateToken(new JsonObject(), new JWTOptions().setExpiresInMinutes(60L)));
                  routingContext.response()
                    .setStatusCode(200)
                    .putHeader("Content-Type", "application/json; charset=utf-8")
                    //.putHeader("Content-Length", String.valueOf(userResult.toString().length()))
                    .end(Json.encodePrettily(loggedInUser.toJson()));
                }else{
                  System.out.println("Did Not Find User");
                  LoginError loginError = new LoginError(ErrorMessages.AUTHENTICATION_ERROR_LOGIN);
                  routingContext.response().setStatusCode(422)
                    .putHeader("content-type", "application/json; charset=utf-8")
                    //.putHeader("Content-Length", String.valueOf(loginError.toString().length()))
                    .end(Json.encodePrettily(new AuthenticationError(ErrorMessages.AUTHENTICATION_ERROR_DEFAULT)));
                }
              });
            }else{
              System.out.println("Did Not Authenticate User");
              LoginError loginError = new LoginError(ErrorMessages.AUTHENTICATION_ERROR_LOGIN);
              routingContext.response().setStatusCode(422)
                .putHeader("content-type", "application/json; charset=utf-8")
                //.putHeader("Content-Length", String.valueOf(loginError.toString().length()))
                .end(Json.encodePrettily(loginError));
            }
        });

  }

  private void getCurrentUser(RoutingContext routingContext) {

    User user = new User();
    user.setEmail("user@email.com");
    user.setPassword("password");
    user.setUsername("username");
    // create and return our response
    routingContext.response()
      .setStatusCode(201)
      .putHeader("content-type", "application/json; charset=utf-8")
      .end(Json.encodePrettily(user));
  }

  private void registerUser(RoutingContext routingContext) {

    // marshall our payload into a User object
    //final User user = Json.decodeValue(routingContext.getBodyAsString(), User.class);
    JsonObject jsonObject = routingContext.getBodyAsJson();
    final User user = new User(jsonObject.getJsonObject("user"));

    // insert into the authentication collection
    loginAuthProvider.insertUser(user.getEmail(), user.getPassword(), null, null, res -> {

      if (res.succeeded()) {
          // now save to the conduit_users collection
          user.set_id(res.result());
          mongoClient.insert(MongoConstants.COLLECTION_NAME_USERS, user.toMongoJson(), r ->{
            if (r.succeeded()) {
              routingContext.response()
                .setStatusCode(201)
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(user.toJson()));
            }else{
              routingContext.response()
                .setStatusCode(422)
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(new RegistrationError(r.cause().getMessage())));
            }
          });
        } else {
          routingContext.response()
            .setStatusCode(422)
            .putHeader("content-type", "application/json; charset=utf-8")
            .end(Json.encodePrettily(new RegistrationError(res.cause().getMessage())));
        }
      });
  }

}
