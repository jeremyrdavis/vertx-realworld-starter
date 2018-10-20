package io.vertx.conduit.users;

import io.vertx.conduit.*;
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
public class GetCurrentUserTest extends BaseDatabaseVerticleTest{

  @Test
  public void testGetCurrentUser(TestContext testContext){

    Async getAsync = testContext.async();

    webClient.get(8080, "localhost", "/api/user")
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
          JsonObject returnedUser = returnedJson.getJsonObject("user");
          testContext.assertEquals("Jacob", returnedUser.getString("username"));
          testContext.assertEquals("jake@jake.jake", returnedUser.getString("email"), "Email should be 'jake@jake.jake");
          testContext.assertEquals("I work at state farm", returnedUser.getString("bio"), "Bio should be I work at state farm");
          testContext.assertNull(returnedUser.getString("image"), "image should be null/empty");
          getAsync.complete();
        }
      });

  }
}
