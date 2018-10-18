package io.vertx.conduit.users;

import io.vertx.conduit.*;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.WebClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static io.vertx.conduit.TestProps.DB_CONNECTION_STRING_TEST;
import static io.vertx.conduit.TestProps.DB_NAME_TEST;

@RunWith(VertxUnitRunner.class)
public class GetProfileTest extends BaseDatabaseVerticleTest{

  @Test
  public void testGetProfile(TestContext testContext) {
    Async getAsync = testContext.async();

    webClient.get(8080, "localhost", "/api/profiles/Jacob")
      .putHeader(HttpProps.CONTENT_TYPE, HttpProps.JSON)
      .putHeader(HttpProps.XREQUESTEDWITH, HttpProps.XMLHTTPREQUEST)
      .putHeader(HttpProps.AUTHORIZATION, TestProps.TOKEN_JACOB)
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

/*
  @Test
  public void testGetNonExistantUser(TestContext testContext) {
    Async getAsync = testContext.async();

    webClient.get(8080, "localhost", "/api/profiles/BuddyTheElf")
      .putHeader(HttpProps.CONTENT_TYPE, HttpProps.JSON)
      .putHeader(HttpProps.XREQUESTEDWITH, HttpProps.XMLHTTPREQUEST)
      .putHeader(HttpProps.AUTHORIZATION, token)
      .send(ar ->{
        if (ar.failed()) {
          testContext.assertEquals(true, ar.succeeded(), "The call should have succeeded");
          testContext.fail(ar.cause());
          getAsync.complete();
        }else{
          testContext.assertEquals(404, ar.result().statusCode());
          getAsync.complete();
        }
      });
  }
*/
}
