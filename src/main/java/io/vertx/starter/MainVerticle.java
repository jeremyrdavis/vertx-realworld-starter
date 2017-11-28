package io.vertx.starter;

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
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class MainVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);

  // for DB access
  private MongoClient mongoClient;

  // MongoDB Collection key for users
  public static final String USER_COLLECTION = "users";

  @Override
  public void start(Future<Void> future) {

    // Configure the MongoClient inline.  This should be externalized into a config file
    mongoClient = MongoClient.createShared(vertx, new JsonObject().put("db_name", "users").put("connection_string", "mongodb://localhost:12345"));

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
    final User user = new User(routingContext.getBodyAsJson());

    //TODO: save this to the database
    mongoClient.insert(USER_COLLECTION, user.toJson(), r -> {
      LOGGER.debug(r.result());
      user.setId(r.result());
      routingContext.response()
        .setStatusCode(201)
        .putHeader("content-type","application/json; charset=utf-8")
        .end(Json.encodePrettily(user));
    });

  }

}
