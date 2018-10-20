package io.vertx.conduit;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class MainVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);


    @Override
    public void start(Future<Void> startFuture) {

        getConfig().setHandler(c ->{
            if (c.succeeded()) {
                LOGGER.info("Configuration retrieved: " + config().getString("env"));
                DeploymentOptions deploymentOptions = new DeploymentOptions().setConfig(c.result());
                CompositeFuture.all(
                        deployVerticle(HttpVerticle.class, deploymentOptions),
                        deployVerticle(UserDAV.class, deploymentOptions)).setHandler(ar2 -> {
                    if (ar2.succeeded()) {
                        LOGGER.info("all deployments succeeded");
                        startFuture.complete();
                    } else {
                        LOGGER.info("deployment failure: " + ar2.cause().getMessage());
                        startFuture.fail(ar2.cause());
                    }
                });
            } else {
                startFuture.fail(c.cause());
            }
        });
    }

    private Future<Void> deployVerticle(Class clazz, DeploymentOptions options) {
        Future<Void> retVal = Future.future();

        vertx.deployVerticle(clazz, options, ar -> {
            if (ar.succeeded()) {
                retVal.complete();
            } else {
                retVal.fail(ar.cause());
            }
        });
        return retVal;
    }

    private Future<JsonObject> getConfig() {
        // Load the default configuration from the classpath
        ConfigStoreOptions localConfig = new ConfigStoreOptions()
                .setType("file")
                .setFormat("json")
                .setConfig(new JsonObject().put("path", "application-config.json"));
        // Add the default and container config options into the ConfigRetriever
        ConfigRetrieverOptions retrieverOptions = new ConfigRetrieverOptions()
                .addStore(localConfig);
        ConfigRetriever configRetriever = ConfigRetriever.create(vertx, retrieverOptions);
        return ConfigRetriever.getConfigAsFuture(configRetriever);
    }

}
