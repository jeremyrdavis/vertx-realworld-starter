package io.vertx.conduit.users;

import io.vertx.conduit.users.models.User;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.WebClient;
import io.vertx.conduit.DBSetupVerticle;
import io.vertx.conduit.MainVerticle;
import org.junit.*;
import org.junit.runner.RunWith;

import static io.vertx.conduit.TestProps.DB_CONNECTION_STRING_TEST;
import static io.vertx.conduit.TestProps.DB_NAME_TEST;

@RunWith(VertxUnitRunner.class)
public class RegisterUserTest {

    private Vertx vertx;

    @Before
    public void setUp(TestContext tc) {
        vertx = Vertx.vertx();

        DeploymentOptions options = new DeploymentOptions()
                .setConfig(new JsonObject()
                        .put("http.port", 8080)
                        .put("db_name", DB_NAME_TEST)
                        .put("connection_string", DB_CONNECTION_STRING_TEST)
                );

        vertx.deployVerticle(new DBSetupVerticle(), ar -> {
            if (ar.succeeded()) {
                System.out.println("DBSetupVerticle complete");
                vertx.deployVerticle(MainVerticle.class.getName(), options, tc.asyncAssertSuccess());
            } else {
                tc.fail(ar.cause());
            }
        });

    }

    @After
    public void tearDown(TestContext tc) {
        vertx.close(tc.asyncAssertSuccess());
    }

    @Test
    public void testRegisteringAUser(TestContext tc) {
        Async async = tc.async();

        User user = new User("username", "user@domain.com", "password");
        final String length = String.valueOf(user.toJson().toString().length());

        WebClient webClient = WebClient.create(vertx);

        webClient.post(8080, "localhost", "/api/users")
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
