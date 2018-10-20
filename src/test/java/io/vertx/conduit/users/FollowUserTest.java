package io.vertx.conduit.users;

import io.vertx.conduit.BaseConduitVerticleTest;
import io.vertx.conduit.HttpProps;
import io.vertx.conduit.TestProps;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(VertxUnitRunner.class)
public class FollowUserTest extends BaseConduitVerticleTest {

  /**
   * Tests the endpoint "/api/profiles/:username/follow"
   *
   * This test verifies that User1 can follow Jacob
   *
   * @param tc
   */
  @Test
  public void testFollowUser(TestContext tc) {

    Async async = tc.async();

    webClient.post(8080, "localhost", "/api/profiles/Jacob/follow")
      .putHeader(HttpProps.CONTENT_TYPE, HttpProps.JSON)
      .putHeader(HttpProps.XREQUESTEDWITH, HttpProps.XMLHTTPREQUEST)
      .putHeader(HttpProps.AUTHORIZATION, TestProps.TOKEN_USER1)
      .send(ar -> {

      if (ar.failed()) {
        async.complete();
        tc.fail(ar.cause());
      }else{
        tc.assertEquals(200, ar.result().statusCode());
        JsonObject returnedJson = ar.result().bodyAsJsonObject();
        tc.assertNotNull(returnedJson);
        JsonObject returnedUser = returnedJson.getJsonObject("profile");
        verifyProfile(returnedUser);
        async.complete();
      }
    });
  }

  void verifyProfile(JsonObject profileToVerify) {
    assertEquals("Jacob", profileToVerify.getString("username"));
    assertEquals("I work at state farm", profileToVerify.getString("bio"));
    assertTrue(profileToVerify.containsKey("image"));
    assertFalse(profileToVerify.containsKey("token"));
    assertFalse(profileToVerify.containsKey("email"));
    assertFalse(profileToVerify.containsKey("password"));
  }

}
