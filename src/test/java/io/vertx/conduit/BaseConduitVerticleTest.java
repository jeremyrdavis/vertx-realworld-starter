package io.vertx.conduit;

import io.vertx.conduit.users.ArticleDAV;
import io.vertx.conduit.users.models.User;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.WebClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

import static io.vertx.conduit.TestProps.DB_CONNECTION_STRING_TEST;
import static io.vertx.conduit.TestProps.DB_NAME_TEST;

@RunWith(VertxUnitRunner.class)
public class BaseConduitVerticleTest {

  protected Vertx vertx;

  protected WebClient webClient;

  protected User jacob;

  protected User user1;

  protected Article testArticle1;

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

    jacob = new User("jake@jake.jake", "jakejake", "Jacob", "I work at state farm", null);
    testArticle1 = new Article("Test Article 1", "Test description 1", "Lorem ipsum dolor site amet.", new ArrayList<String>(3){ { add("test1"); add("test2"); add("test3"); } });

    vertx.deployVerticle(DBSetupVerticle.class.getName(), tc.asyncAssertSuccess());
    vertx.deployVerticle(HttpVerticle.class.getName(), options, tc.asyncAssertSuccess());
    vertx.deployVerticle(UserDAV.class.getName(), options, tc.asyncAssertSuccess());
    vertx.deployVerticle(ArticleDAV.class.getName(), options, tc.asyncAssertSuccess());
  }

  @Test
  public void testSetup(TestContext testContext){
    System.out.println("base setup complete");
    testContext.assertTrue(true);
  }

  @After
  public void tearDown(TestContext tc) {
    vertx.setTimer(1000, t -> { System.out.println("timer complete"); });
    vertx.close(tc.asyncAssertSuccess());

  }


}
