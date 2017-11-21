package io.vertx.conduit.users;

import io.vertx.conduit.users.models.User;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.starter.MainVerticle;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class RegisterUserTest {

  private Vertx vertx;

  @Before
  public void setUp(TestContext tc) {
    vertx = Vertx.vertx();
    vertx.deployVerticle(MainVerticle.class.getName(), tc.asyncAssertSuccess());
  }

  @After
  public void tearDown(TestContext tc) {
    vertx.close(tc.asyncAssertSuccess());
  }

  @Test
  public void testRegisteringAUser(TestContext tc) {
    Async async = tc.async();

    final String json = Json.encodePrettily(new User("username","user@domain.com", "password"));
    final String length = String.valueOf(json.length());

    vertx.createHttpClient().post(8080, "localhost", "/api/users")
      .putHeader("content-type", "application/json")
      .putHeader("content-length", length)
      .handler(response -> {
          tc.assertEquals(response.statusCode(), 201);
          tc.assertTrue(response.headers().get("content-type").contains("application/json"));
          response.bodyHandler(body -> {
            final User user = Json.decodeValue(body.toString(), User.class);
            tc.assertEquals(user.getUsername(), "username");
            tc.assertEquals(user.getEmail(), "user@domain.com");
          });
          async.complete();
      }).write(json).end();
  }
}
