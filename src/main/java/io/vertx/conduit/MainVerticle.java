package io.vertx.conduit;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

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
