package io.vertx.conduit.users;


import io.vertx.conduit.BaseConduitVerticleTest;
import io.vertx.conduit.HttpProps;
import io.vertx.conduit.TestProps;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UnfollowUserTest extends BaseConduitVerticleTest {


    @Test
    public void testUnfollowUser(TestContext tc) {
        Async async = tc.async();

        webClient.delete(8080, "localhost", "/api/profiles/Jacob/follow")
            .putHeader(HttpProps.CONTENT_TYPE, HttpProps.JSON)
            .putHeader(HttpProps.XREQUESTEDWITH, HttpProps.XMLHTTPREQUEST)
            .putHeader(HttpProps.AUTHORIZATION, TestProps.TOKEN_USER1)
            .send(ar -> {
                if (ar.failed()) {
                    async.complete();
                    tc.fail(ar.cause());
                } else {
                    tc.assertEquals(200, ar.result().statusCode());
                    JsonObject returnedJson = ar.result().bodyAsJsonObject();
                    tc.assertNotNull(returnedJson);
                    JsonObject returnedUser = returnedJson.getJsonObject("profile");
                    verifyProfile(returnedUser);
                    assertFalse(returnedJson.containsKey("following"));
                    async.complete();
                }
            });

    }

    void verifyProfile(JsonObject profileToVerify) {
        assertEquals("User1", profileToVerify.getString("username"));
        assertEquals("I am User1", profileToVerify.getString("bio"));
        assertTrue(profileToVerify.containsKey("image"));
        assertFalse(profileToVerify.containsKey("token"));
        assertFalse(profileToVerify.containsKey("email"));
        assertFalse(profileToVerify.containsKey("password"));
        assertFalse(profileToVerify.containsKey("following"));
    }
}
