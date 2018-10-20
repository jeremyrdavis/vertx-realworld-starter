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

@RunWith(VertxUnitRunner.class)
public class UpdateUserTest extends BaseConduitVerticleTest {

    @Test
    public void testJWTTokenRequired(TestContext testContext) {
        Async async = testContext.async();

        webClient.put(8080, "localhost", "/api/user")
                .putHeader(HttpProps.CONTENT_TYPE, HttpProps.JSON)
                .putHeader(HttpProps.XREQUESTEDWITH, HttpProps.XMLHTTPREQUEST)
                .sendJsonObject(new JsonObject()
                        .put("user", new JsonObject()
                                .put("username", jacob.getUsername())
                                .put("email", jacob.getEmail())
                                .put("image", "https://i.stack.imgur.com/xHWG8.jpg")
                        ), ar -> {
                    if (ar.succeeded()) {
                        testContext.assertEquals(401, ar.result().statusCode());
                        async.complete();
                    } else {
                        testContext.fail(ar.cause());
                    }

                });
    }

    @Test
    public void testUpdatingAUser(TestContext testContext) {
        Async async = testContext.async();

        webClient.put(8080, "localhost", "/api/user")
                .putHeader(HttpProps.CONTENT_TYPE, HttpProps.JSON)
                .putHeader(HttpProps.XREQUESTEDWITH, HttpProps.XMLHTTPREQUEST)
                .putHeader(HttpProps.AUTHORIZATION, TestProps.TOKEN_JACOB)
                .sendJsonObject(new JsonObject()
                        .put("user", new JsonObject()
                                .put("username", jacob.getUsername())
                                .put("email", jacob.getEmail())
                                .put("image", "https://i.stack.imgur.com/xHWG8.jpg")
                        ), ar -> {
                    if (ar.succeeded()) {
                        testContext.assertEquals(200, ar.result().statusCode());
                        System.out.println("returned: " + ar.result().bodyAsJsonObject());
                        JsonObject returnedJson = ar.result().bodyAsJsonObject();
                        testContext.assertNotNull(returnedJson);
                        JsonObject returnedUser = returnedJson.getJsonObject("user");
                        testContext.assertEquals(jacob.getUsername(), returnedUser.getString("username"), "Username should be '" + jacob.getUsername() + "'");
                        testContext.assertEquals(jacob.getEmail(), returnedUser.getString("email"), "Email should be '" + jacob.getEmail() +"'");
                        testContext.assertEquals(jacob.getBio(), returnedUser.getString("bio"), "Bio should be '" + jacob.getBio() + "'");
                        testContext.assertNotNull(returnedUser.getString("image"), "image should not be null/empty");
                        testContext.assertEquals("https://i.stack.imgur.com/xHWG8.jpg", returnedUser.getString("image"));
                        async.complete();
                    }else{
                        testContext.fail(ar.cause());
                    }

                });

    }
}
