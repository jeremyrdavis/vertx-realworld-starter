package io.vertx.conduit.users;

import io.vertx.conduit.BaseDatabaseVerticleTest;
import io.vertx.conduit.HttpProps;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.WebClient;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class LoginUserTest extends BaseDatabaseVerticleTest {

  /*
  @Test
  public void webClientTest(TestContext testContext) {

    Async async = testContext.async();

    JsonObject json = new TestUser("email@domain.com", "password").toLoginJson();
    final String length = String.valueOf(json.toString().length());


    HttpClientRequest req = vertx.createHttpClient(new HttpClientOptions()
      .setSsl(true)
      .setTrustAll(true))
      .post(8080, "localhost", "/api/users/login");

    req.handler(res ->{
      testContext.assertTrue(res.statusCode(), 200);
      HttpClientResponse httpClientResponse = res.bodyHandler();
      httpClientResponse.
    });


    WebClient webClient = WebClient.create(vertx);
    webClient.post(8080, "localhost/api/users/login", json.toString()).send(ar ->{
      testContext.assertTrue(ar.succeeded());
      HttpResponse<Buffer> response = ar.result();
      final User conduitUser = Json.decodeValue(response.toString(), User.class);
      System.out.println("Returned value: " + conduitUser.toJson());
      testContext.assertNotNull(conduitUser);
    });

  }
  */

    @Test
    public void testLoggingIn(TestContext testContext) {

        Async async = testContext.async();

        WebClient webClient = WebClient.create(vertx);

        webClient.post(8080, "localhost", "/api/users/login")
                .putHeader(HttpProps.CONTENT_TYPE, HttpProps.JSON)
                .putHeader(HttpProps.XREQUESTEDWITH, HttpProps.XMLHTTPREQUEST)
                .sendJsonObject(new JsonObject()
                        .put("user", new JsonObject()
                                .put("username", "Jacob")
                                .put("email", "jake@jake.jake")
                                .put("password", "jakejake")
                        ), ar -> {
                    if (ar.succeeded()) {
                        testContext.assertEquals(200, ar.result().statusCode());
                        System.out.println(ar.result().bodyAsJsonObject());
                        JsonObject returnedJson = ar.result().bodyAsJsonObject();
                        testContext.assertNotNull(returnedJson);
                        JsonObject returnedUser = returnedJson.getJsonObject("user");
                        testContext.assertEquals("Jacob", returnedUser.getString("username"));
                        testContext.assertEquals("jake@jake.jake", returnedUser.getString("email"));
                        testContext.assertEquals("I work at state farm", returnedUser.getString("bio"));
                        async.complete();
                    } else {
                        testContext.fail(ar.cause());
                    }

                });

/*
        vertx.createHttpClient()
                .post(8080, "localhost", "/api/users/login")
                .putHeader("content-type", "application/json")
                .putHeader("content-length", length)
                .handler(response -> {
                    testContext.assertEquals(response.statusCode(), 200);
                    testContext.assertTrue(response.headers().get("content-type").contains("application/json"));
                    response.bodyHandler(body -> {
                        testContext.assertNotNull(body);
                        System.out.println("Returned body: " + body.toString());

                        final User user = new User(body.toJsonObject().getJsonObject("user"));
                        System.out.println("Returned value: " + user.toJson());

                        testContext.assertEquals("email@domain.com", user.getEmail());
                        testContext.assertNotNull(user.getUsername(), "username should not be null");
                        testContext.assertNull(user.get_id());
                        testContext.assertNull(user.getPassword());
                        testContext.assertNotNull(user.getToken(), "token should not be null");
                        //testContext.assertTrue(user.getToken().length() >= 1);

                        async.complete();
                    });
                }).write(json.toString()).end();
*/

    }

/*
  @Test
  public void testInvalidLogin(TestContext testContext) {
    Async async = testContext.async();

    JsonObject json = new TestUser("wrongemail@domain.com", "password").toLoginJson();
    final String length = String.valueOf(json.toString().length());

    vertx.createHttpClient().post(8080, "localhost", "/api/users/login")
      .putHeader("content-type", "application/json")
      .putHeader("content-length", length)
      .handler(response -> {
        //make sure that we return the appropriate HTTP response code
        testContext.assertFalse(response.statusCode() == 200);
        testContext.assertEquals(response.statusCode(), 422);
        testContext.assertTrue(response.headers().get("content-type").contains("application/json"));
        response.bodyHandler(body -> {
          testContext.assertNotNull(body);
          final LoginError loginError = Json.decodeValue(body.toString(), LoginError.class);
          System.out.println("Returned value: " + loginError.toJson());
          testContext.assertEquals(loginError.getMessage(), ErrorMessages.AUTHENTICATION_ERROR_LOGIN);
        });
        async.complete();
      }).write(json.toString()).end();

  }
*/


}
