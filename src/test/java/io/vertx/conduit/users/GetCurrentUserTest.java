package io.vertx.conduit.users;

import io.vertx.conduit.DBSetupVerticle;
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
public class GetCurrentUserTest {

  private Vertx vertx;

  private WebClient webClient;

  String token = "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJlbWFpbCI6Impha2VAamFrZS5qYWtlIiwicGFzc3dvcmQiOiJqYWtlamFrZSIsImlhdCI6MTUzOTczNTQwM30.YBhmIStiM_909UNGWYo3kQk_K6no2yp2VQONlQzXpPk";

  @Before
  public void setUp(TestContext testContext) {

    vertx = Vertx.vertx();

    DeploymentOptions options = new DeploymentOptions()
      .setConfig(new JsonObject()
        .put("http.port", 8080)
        .put("db_name", DB_NAME_TEST)
        .put("connection_string", DB_CONNECTION_STRING_TEST)
      );

    vertx.deployVerticle(DBSetupVerticle.class.getName(), testContext.asyncAssertSuccess());
    vertx.deployVerticle(HttpVerticle.class.getName(), options, testContext.asyncAssertSuccess());
    vertx.deployVerticle(MongoVerticle.class.getName(), options, testContext.asyncAssertSuccess());

    webClient = WebClient.create(vertx);

/*
    webClient.get(8080, "localhost", "/api/user")
      .putHeader("Authorization", token)
      .sendJsonObject(new JsonObject()
        .put("user", new JsonObject()
          .put("username", "Jacob")
          .put("email", "jake@jake.jake")
          .put("password", "jakejake")
        ), ar -> {
          if (ar.succeeded()) {
            loginAsync.complete();
          }else{
            testContext.fail(ar.cause());
            loginAsync.complete();
          }
        });
*/
  }

  @Test
  public void testGetCurrentUser(TestContext testContext){

    Async getAsync = testContext.async();

    webClient.get(8080, "localhost", "/api/user")
      .putHeader("Authorization", token)
      .send(ar ->{
        if (ar.failed()) {
          testContext.assertEquals(true, ar.succeeded(), "The call should have succeeded");
          testContext.fail(ar.cause());
          getAsync.complete();
        }else{
          testContext.assertEquals(200, ar.result().statusCode());
          JsonObject returnedJson = ar.result().bodyAsJsonObject();
          testContext.assertNotNull(returnedJson);
          JsonObject returnedUser = returnedJson.getJsonObject("user");
          testContext.assertEquals("Jacob", returnedUser.getString("username"));
          getAsync.complete();
        }
      });

  }
}
