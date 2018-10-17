package io.vertx.conduit.users;

import io.vertx.conduit.DBSetupVerticle;
import io.vertx.conduit.HttpProps;
import io.vertx.conduit.HttpVerticle;
import io.vertx.conduit.MongoVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.WebClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static io.vertx.conduit.TestProps.DB_CONNECTION_STRING_TEST;
import static io.vertx.conduit.TestProps.DB_NAME_TEST;

@RunWith(VertxUnitRunner.class)
public class GetProfileTest {

  Vertx vertx;

  private WebClient webClient;

  String token = "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJlbWFpbCI6Impha2VAamFrZS5qYWtlIiwicGFzc3dvcmQiOiJqYWtlamFrZSIsImlhdCI6MTUzOTczNTQwM30.YBhmIStiM_909UNGWYo3kQk_K6no2yp2VQONlQzXpPk";

  @Before
  public void setUp(TestContext testContext){
    vertx = Vertx.vertx();

    webClient = WebClient.create(vertx);

    DeploymentOptions options = new DeploymentOptions()
      .setConfig(new JsonObject()
        .put("http.port", 8080)
        .put("db_name", DB_NAME_TEST)
        .put("connection_string", DB_CONNECTION_STRING_TEST)
      );

    vertx.deployVerticle(DBSetupVerticle.class.getName(), testContext.asyncAssertSuccess());
    vertx.deployVerticle(HttpVerticle.class.getName(), options, testContext.asyncAssertSuccess());
    vertx.deployVerticle(MongoVerticle.class.getName(), options, testContext.asyncAssertSuccess());
  }

  @Test
  public void testGetProfile(TestContext testContext) {
    Async getAsync = testContext.async();

    webClient.get(8080, "localhost", "/api/profiles/Jacob")
      .putHeader(HttpProps.CONTENT_TYPE, HttpProps.JSON)
      .putHeader(HttpProps.XREQUESTEDWITH, HttpProps.XMLHTTPREQUEST)
      .putHeader(HttpProps.AUTHORIZATION, token)
      .send(ar ->{
        if (ar.failed()) {
          testContext.assertEquals(true, ar.succeeded(), "The call should have succeeded");
          testContext.fail(ar.cause());
          getAsync.complete();
        }else{
          testContext.assertEquals(200, ar.result().statusCode());
          JsonObject returnedJson = ar.result().bodyAsJsonObject();
          testContext.assertNotNull(returnedJson);
          JsonObject returnedUser = returnedJson.getJsonObject("profile");
          testContext.assertEquals("Jacob", returnedUser.getString("username"));
          testContext.assertEquals("I work at state farm", returnedUser.getString("bio"));
          testContext.assertTrue(returnedUser.containsKey("image"));
          testContext.assertFalse(returnedUser.containsKey("token"));
          testContext.assertFalse(returnedUser.containsKey("email"));
          testContext.assertFalse(returnedUser.containsKey("password"));
          getAsync.complete();
        }
      });

  }
}
