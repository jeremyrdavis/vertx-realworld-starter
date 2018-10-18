package io.vertx.conduit;

import io.vertx.conduit.DBSetupVerticle;
import io.vertx.conduit.HttpVerticle;
import io.vertx.conduit.MongoVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.WebClient;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

import static io.vertx.conduit.TestProps.DB_CONNECTION_STRING_TEST;
import static io.vertx.conduit.TestProps.DB_NAME_TEST;

@RunWith(VertxUnitRunner.class)
public abstract class BaseDatabaseVerticleTest {

  protected Vertx vertx;

  protected WebClient webClient;

  @Before
  public void setUp(TestContext tc) {

    vertx = Vertx.vertx();
    webClient = WebClient.create(vertx);

    DeploymentOptions options = new DeploymentOptions()
      .setConfig(new JsonObject()
        .put("http.port", 8080)
        .put("db_name", DB_NAME_TEST)
        .put("connection_string", DB_CONNECTION_STRING_TEST)
      );

    vertx.deployVerticle(DBSetupVerticle.class.getName(), tc.asyncAssertSuccess());
    vertx.deployVerticle(HttpVerticle.class.getName(), options, tc.asyncAssertSuccess());
    vertx.deployVerticle(MongoVerticle.class.getName(), options, tc.asyncAssertSuccess());
  }

  @After
  public void tearDown(TestContext tc) {
    vertx.setTimer(1000, t -> { System.out.println("timer complete"); });
    vertx.close(tc.asyncAssertSuccess());

  }


}
