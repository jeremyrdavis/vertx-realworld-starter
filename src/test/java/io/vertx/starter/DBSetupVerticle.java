package io.vertx.starter;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class DBSetupVerticle extends AbstractVerticle{

  private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);

  // for DB access
  private MongoClient mongoClient;

  // MongoDB Collection key for users
  public static final String USER_COLLECTION = "users";

  @Override
  public void start(Future<Void> future) {

    // Configure the MongoClient inline.  This should be externalized into a config file
    mongoClient = MongoClient.createShared(vertx, new JsonObject().put("db_name", "conduit").put("connection_string", "mongodb://localhost:12345"));

  }
}
