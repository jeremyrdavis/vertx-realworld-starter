package io.vertx.conduit;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class MainVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);


    @Override
    public void start(Future<Void> startFuture) {

        DeploymentOptions deploymentOptions = new DeploymentOptions().setConfig(config());
        CompositeFuture.all(
                deployVerticle(HttpVerticle.class, deploymentOptions),
                deployVerticle(UserDAV.class, deploymentOptions)).setHandler(ar ->{
            if (ar.succeeded()) {
                LOGGER.info("all deployments succeeded");
                startFuture.complete();
            }else{
                LOGGER.info("deployment failure: " + ar.cause().getMessage());
                startFuture.fail(ar.cause());
            }
        });
    }

    private Future<Void> deployVerticle(Class clazz, DeploymentOptions options) {
        Future<Void> retVal = Future.future();

        vertx.deployVerticle(clazz, options, ar -> {
            if (ar.succeeded()) {
                retVal.complete();
            }else{
                retVal.fail(ar.cause());
            }
        });
        return retVal;
    }

}
