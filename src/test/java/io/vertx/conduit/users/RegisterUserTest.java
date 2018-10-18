package io.vertx.conduit.users;

import io.vertx.conduit.BaseDatabaseVerticleTest;
import io.vertx.conduit.HttpProps;
import io.vertx.conduit.users.models.User;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.WebClient;
import org.junit.*;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class RegisterUserTest extends BaseDatabaseVerticleTest {

    @Test
    public void testRegisteringAUser(TestContext tc) {
        Async async = tc.async();

        User user = new User("username", "user@domain.com", "password");
        final String length = String.valueOf(user.toJson().toString().length());

        WebClient webClient = WebClient.create(vertx);

        webClient.post(8080, "localhost", "/api/users")
                .putHeader(HttpProps.CONTENT_TYPE, HttpProps.JSON)
                .putHeader(HttpProps.XREQUESTEDWITH, HttpProps.XMLHTTPREQUEST)
                .sendJsonObject(new JsonObject()
                        .put("user", new JsonObject()
                                .put("username", "User2")
                                .put("email", "user2@user2.user2")
                                .put("password", "user2user2")
                        ), ar -> {
                    if (ar.succeeded()) {
                        tc.assertEquals(201, ar.result().statusCode());
                        System.out.println("returned: " + ar.result().bodyAsJsonObject());
                        JsonObject returnedJson = ar.result().bodyAsJsonObject();
                        tc.assertNotNull(returnedJson);
                        JsonObject returnedUser = returnedJson.getJsonObject("user");
                        tc.assertEquals("User2", returnedUser.getString("username"), "Username should be 'User2");
                        tc.assertEquals("user2@user2.user2", returnedUser.getString("email"), "Email should be 'user2@user2.user2");
                        tc.assertNull(returnedUser.getString("bio"), "Bio should be null/empty");
                        tc.assertNull(returnedUser.getString("image"), "image should be null/empty");
                        tc.assertNotNull(returnedUser.getString("token", "Token should not be null/empty"));
                        async.complete();
                    }else{
                        tc.fail(ar.cause());
                    }

                });

    }

}
