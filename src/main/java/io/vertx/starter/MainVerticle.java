package io.vertx.starter;

import io.vertx.conduit.users.models.User;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start(Future<Void> future) {

    // create a router to handle the API
    Router router = Router.router(vertx);

    router.route("/").handler(routingContext -> {
      HttpServerResponse response = routingContext.response();
      response.putHeader("content-type", "text/plain").end("Hello Vert.x!");
    });

    router.route("/api/user*").handler(BodyHandler.create());
    router.get("/api/user").handler(this::getCurrentUser);
    router.post("/api/users").handler(this::registerUser);

    vertx.createHttpServer()
      .requestHandler(router::accept)
      .listen(8080, result -> {
        if (result.succeeded()) {
          future.complete();
        }else {
          future.fail(result.cause());
        }
      });
  }

  private void getCurrentUser(RoutingContext routingContext) {

    User user = new User("username", "user@email.com", "foo");
    // create and return our response
    routingContext.response()
      .setStatusCode(201)
      .putHeader("content-type", "application/json; charset=utf-8")
      .end(Json.encodePrettily(user));
  }

  private void registerUser(RoutingContext routingContext) {

    // marshall our payload into a User object
    final User user = Json.decodeValue(routingContext.getBodyAsString(), User.class);

    //TODO: save this to the database

    // create and return our response
    routingContext.response()
      .setStatusCode(201)
      .putHeader("content-type", "application/json; charset=utf-8")
      .end(Json.encodePrettily(user));

  }

}
