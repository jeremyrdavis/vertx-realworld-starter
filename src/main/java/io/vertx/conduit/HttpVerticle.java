package io.vertx.conduit;

import com.sun.xml.internal.bind.v2.runtime.unmarshaller.XsiNilLoader;
import io.vertx.conduit.errors.AuthenticationError;
import io.vertx.conduit.errors.ErrorMessages;
import io.vertx.conduit.errors.RegistrationError;
import io.vertx.conduit.users.models.User;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

import static io.vertx.conduit.MongoVerticle.*;

public class HttpVerticle extends AbstractVerticle {

  // Authentication provider for the api
  private JWTAuth jwtAuth;


  @Override
  public void start(Future<Void> startFuture) {

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
    apiRouter.get("/user").handler(this::getCurrentUser);
    apiRouter.post("/users").handler(this::registerUser);
    apiRouter.post("/users/login").handler(this::loginUser);
    apiRouter.get("/profiles/:username").handler(this::getProfile);
    apiRouter.post("/profiles/:username/follow").handler(this::followUser);
    baseRouter.mountSubRouter("/api", apiRouter);

//    new HttpServerOptions()
//      .setSsl(true)
//      .setKeyStoreOptions(new JksOptions().setPath("server-keystore.jks").setPassword("secret")))
    vertx.createHttpServer()
      .requestHandler(baseRouter::accept)
      .listen(8080, result -> {
        if (result.succeeded()) {
          startFuture.complete();
        } else {
          startFuture.fail(result.cause());
        }
      });

  }

  private void followUser(RoutingContext routingContext) {

    String headerAuth = routingContext.request().getHeader("Authorization");
    System.out.println("headerAuth: " + headerAuth);

    String[] values = headerAuth.split(" ");
    System.out.println("values[1]: " + values[1]);

    jwtAuth.authenticate(new JsonObject()
      .put("jwt", values[1]), res -> {
      if (res.succeeded()) {
        io.vertx.ext.auth.User theUser = res.result();
        JsonObject principal = theUser.principal();
      }
    });

    String username = routingContext.request().getParam("username");
    if (username == null || username.isEmpty()) {
      routingContext.response().setStatusCode(400).end();
    } else {
      JsonObject message = new JsonObject()
        .put(MESSAGE_ACTION, MESSAGE_ACTION_FOLLOW_USER)
        .put(MESSAGE_FOLLOW_USER_FOLLOWED_USER, username);
    }
  }


  private void getProfile(RoutingContext routingContext) {
    String username = routingContext.request().getParam("username");
    if (username == null || username.isEmpty()) {
      routingContext.response().setStatusCode(400).end();
    } else {
      JsonObject message = new JsonObject()
        .put(MESSAGE_ACTION, MESSSAGE_ACTION_LOOKUP_USER_BY_USERNAME)
        .put(MESSAGE_ACTION_LOOKUP_USER_BY_USERNAME_VALUE, username);

      vertx.eventBus().send(MESSAGE_ADDRESS, message, ar -> {

        if (ar.succeeded()) {
          JsonObject userJson = ((JsonObject) ar.result().body()).getJsonObject(MESSAGE_RESPONSE_DETAILS);
          final User returnedUser = new User(userJson);
          routingContext.response()
            .setStatusCode(200)
            .putHeader("Content-Type", "application/json; charset=utf-8")
            //.putHeader("Content-Length", String.valueOf(userResult.toString().length()))
            .end(Json.encodePrettily(returnedUser.toProfileJson()));
        } else {
          System.out.println("Did Not Find User");
          routingContext.response().setStatusCode(422)
            .putHeader("content-type", "application/json; charset=utf-8")
            //.putHeader("Content-Length", String.valueOf(loginError.toString().length()))
            .end(Json.encodePrettily(new AuthenticationError(ErrorMessages.AUTHENTICATION_ERROR_DEFAULT + " " + ar.cause().getMessage())));
        }
      });
    }
  }

  private void getCurrentUser(RoutingContext routingContext) {

    String headerAuth = routingContext.request().getHeader("Authorization");
    System.out.println("headerAuth: " + headerAuth);

    String[] values = headerAuth.split(" ");
    System.out.println("values[1]: " + values[1]);

    jwtAuth.authenticate(new JsonObject()
      .put("jwt", values[1]), res -> {
      if (res.succeeded()) {
        io.vertx.ext.auth.User theUser = res.result();
        JsonObject principal = theUser.principal();
        System.out.println("theUser: " + theUser.principal().encodePrettily());

        JsonObject message2 = new JsonObject()
          .put(MESSAGE_ACTION, MESSSAGE_ACTION_LOOKUP_USER_BY_EMAIL)
          .put(MESSAGE_ACTION_LOOKUP_USER_BY_EMAIL_VALUE, principal.getString("email"));

        vertx.eventBus().send(MESSAGE_ADDRESS, message2, ar -> {
          if (ar.succeeded()) {
            System.out.println(MESSSAGE_ACTION_LOOKUP_USER_BY_EMAIL + "succeeded");
            JsonObject userJson = ((JsonObject) ar.result().body()).getJsonObject(MESSAGE_RESPONSE_DETAILS);
            final User returnedUser = new User(userJson);
            // get the JWT Token
            returnedUser.setToken(jwtAuth.generateToken(principal, new JWTOptions().setIgnoreExpiration(true)));
            routingContext.response()
              .setStatusCode(200)
              .putHeader("Content-Type", "application/json; charset=utf-8")
              //.putHeader("Content-Length", String.valueOf(userResult.toString().length()))
              .end(Json.encodePrettily(returnedUser.toConduitJson()));
          } else {
            System.out.println("Did Not Find User");
            routingContext.response().setStatusCode(422)
              .putHeader("content-type", "application/json; charset=utf-8")
              //.putHeader("Content-Length", String.valueOf(loginError.toString().length()))
              .end(Json.encodePrettily(new AuthenticationError(ErrorMessages.AUTHENTICATION_ERROR_DEFAULT + " " + ar.cause().getMessage())));
          }
        });

      } else {
        //failed!
        System.out.println("authentication failed ");
      }

    });
  }

  private void registerUser(RoutingContext routingContext) {

    // marshall our payload into a User object
    //final User user = Json.decodeValue(routingContext.getBodyAsString(), User.class);

    final User user = Json.decodeValue(routingContext.getBodyAsJson().getJsonObject("user").toString(), User.class);

    JsonObject message = new JsonObject()
      .put(MESSAGE_ACTION, MESSAGE_ACTION_REGISTER)
      .put(MESSAGE_VALUE_USER, routingContext.getBodyAsJson().getJsonObject("user"));

    System.out.println(message.getJsonObject("user"));
    vertx.eventBus().send(MESSAGE_ADDRESS, message, ar -> {
      if (ar.succeeded()) {
        JsonObject userJson = ((JsonObject) ar.result().body()).getJsonObject(MESSAGE_RESPONSE_DETAILS);
        final User returnedUser = new User(userJson);
        // get the JWT Token
        returnedUser.setToken(jwtAuth.generateToken(new JsonObject().put("email", user.getEmail()).put("password", user.getPassword()), new JWTOptions().setIgnoreExpiration(true)));
        routingContext.response()
          .setStatusCode(201)
          .putHeader("Content-Type", "application/json; charset=utf-8")
          //.putHeader("Content-Length", String.valueOf(userResult.toString().length()))
          .end(Json.encodePrettily(returnedUser.toConduitJson()));
      } else {
        routingContext.response()
          .setStatusCode(422)
          .putHeader("content-type", "application/json; charset=utf-8")
          .end(Json.encodePrettily(new RegistrationError(ar.cause().getMessage())));
      }
    });
    // insert into the authentication collection

/*
    loginAuthProvider.insertUser(user.getEmail(), user.getPassword(), null, null, res -> {

      if (res.succeeded()) {
        // now save to the conduit_users collection
        user.set_id(res.result());
        mongoClient.insert(MongoConstants.COLLECTION_NAME_USERS, user.toMongoJson(), r -> {
          if (r.succeeded()) {
            routingContext.response()
              .setStatusCode(201)
              .putHeader("content-type", "application/json; charset=utf-8")
              .end(Json.encodePrettily(user));
          } else {
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
*/
  }

  private void loginUser(RoutingContext routingContext) {

    final User user = Json.decodeValue(routingContext.getBodyAsJson().getJsonObject("user").toString(), User.class);

    JsonObject authInfo = new JsonObject().put("email", user.getEmail()).put("password", user.getPassword());
    JsonObject message = new JsonObject()
      .put(MESSAGE_ACTION, MESSAGE_ACTION_LOGIN)
      .put(MESSAGE_VALUE_USER, authInfo);

    vertx.eventBus().send(MESSAGE_ADDRESS, message, ar -> {
      if (ar.succeeded()) {

        JsonObject message2 = new JsonObject()
          .put(MESSAGE_ACTION, MESSSAGE_ACTION_LOOKUP_USER_BY_EMAIL)
          .put(MESSAGE_ACTION_LOOKUP_USER_BY_EMAIL_VALUE, authInfo.getString("email"));

        vertx.eventBus().send(MESSAGE_ADDRESS, message2, ar2 -> {
          if (ar2.succeeded()) {
            System.out.println(MESSSAGE_ACTION_LOOKUP_USER_BY_EMAIL + "succeeded");
            JsonObject userJson = ((JsonObject) ar2.result().body()).getJsonObject(MESSAGE_RESPONSE_DETAILS);
            final User returnedUser = new User(userJson);
            // get the JWT Token
            returnedUser.setToken(jwtAuth.generateToken(authInfo, new JWTOptions().setIgnoreExpiration(true)));
            routingContext.response()
              .setStatusCode(200)
              .putHeader("Content-Type", "application/json; charset=utf-8")
              //.putHeader("Content-Length", String.valueOf(userResult.toString().length()))
              .end(Json.encodePrettily(returnedUser.toConduitJson()));
          } else {
            System.out.println("Did Not Find User");
            routingContext.response().setStatusCode(422)
              .putHeader("content-type", "application/json; charset=utf-8")
              //.putHeader("Content-Length", String.valueOf(loginError.toString().length()))
              .end(Json.encodePrettily(new AuthenticationError(ErrorMessages.AUTHENTICATION_ERROR_DEFAULT + " " + ar2.cause().getMessage())));
          }
        });
      } else {
        System.out.println("Did Not Find User");
        routingContext.response().setStatusCode(422)
          .putHeader("content-type", "application/json; charset=utf-8")
          //.putHeader("Content-Length", String.valueOf(loginError.toString().length()))
          .end(Json.encodePrettily(new AuthenticationError(ErrorMessages.AUTHENTICATION_ERROR_DEFAULT)));
      }
    });

  }


}
