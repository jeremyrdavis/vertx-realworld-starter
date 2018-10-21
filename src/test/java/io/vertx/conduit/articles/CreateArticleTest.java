package io.vertx.conduit.articles;

import io.vertx.conduit.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class CreateArticleTest extends BaseConduitVerticleTest {

    @Test
    public void testCreateArticle(TestContext tc) {
        Async async = tc.async();

        webClient.post(8080, "localhost", "/api/articles")
                .putHeader(HttpProps.CONTENT_TYPE, HttpProps.JSON)
                .putHeader(HttpProps.XREQUESTEDWITH, HttpProps.XMLHTTPREQUEST)
                .putHeader(HttpProps.AUTHORIZATION, TestProps.TOKEN_USER1)
                .sendJsonObject(new JsonObject()
                        .put("article", new JsonObject()
                                .put("title", "How to train your dragon")
                                .put("description", "Ever wonder how?")
                                .put("body", "You have to believe")
                                .put("tagList", new JsonArray().add("reactjs").add("angularjs").add("dragons"))
                        ), ar -> {
                    if (ar.succeeded()) {
                        tc.assertEquals(200, ar.result().statusCode());
                        JsonObject returnedJson = ar.result().bodyAsJsonObject();
                        tc.assertNotNull(returnedJson);
                        Article returnedArticle = new Article(returnedJson.getJsonObject("article"));
                        tc.assertEquals("How to train your dragon", returnedArticle.getTitle(), "Title should be 'How to train your dragon'");
                        tc.assertEquals("Ever wonder how?", returnedArticle.getDescription(), "Description should be 'Ever wonder how?'");
                        tc.assertEquals("You have to believe", returnedArticle.getBody(), "Body should be 'You have to believe'");
                        tc.assertNotNull("There should be a slug", returnedArticle.getSlug());
                        tc.assertNotNull(returnedArticle.getTagsList(), "TagsList should not be null");
                        tc.assertEquals(3, returnedArticle.getTagsList().size(), "There should be 3 tags");
                        /*
                        JsonObject returnedUser = returnedJson.getJsonObject("user");
                        tc.assertEquals("User2", returnedUser.getString("username"), "Username should be 'User2");
                        tc.assertEquals("user2@user2.user2", returnedUser.getString("email"), "Email should be 'user2@user2.user2");
                        tc.assertNull(returnedUser.getString("bio"), "Bio should be null/empty");
                        tc.assertNull(returnedUser.getString("image"), "image should be null/empty");
                        tc.assertNotNull(returnedUser.getString("token", "Token should not be null/empty"));
*/
                        async.complete();
                    }else{
                        tc.fail(ar.cause());
                    }

                });


    }
}
