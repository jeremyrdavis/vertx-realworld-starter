package io.vertx.starter;

import io.vertx.conduit.errors.AuthenticationError;
import io.vertx.conduit.errors.ErrorMessages;
import io.vertx.conduit.errors.LoginError;
import io.vertx.conduit.errors.RegistrationError;
import io.vertx.conduit.users.models.MongoConstants;
import io.vertx.conduit.users.models.User;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
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

public class MainVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);


  @Override
  public void start(Future<Void> startFuture) {

      System.out.println("Deploying Verticles");
      System.out.println(config().getString("db_name"));
        vertx.deployVerticle(new HttpVerticle(), ar -> {
            System.out.println("HttpVerticle deployed");
          if (ar.succeeded()) {
              vertx.deployVerticle(new MongoVerticle(), new DeploymentOptions().setConfig(config()), ar2 ->{
                  if (ar2.succeeded()) {
                      startFuture.complete();
                  }else{
                      startFuture.fail(ar.cause());
                  }
              });
          }else{
            startFuture.fail(ar.cause());
          }
        });
  }

}
