package io.vertx.conduit.articles;

import io.vertx.conduit.Article;
import io.vertx.conduit.BaseConduitVerticleTest;
import io.vertx.conduit.HttpProps;
import io.vertx.conduit.TestProps;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

@RunWith(VertxUnitRunner.class)
public class UpdateArticleTest  extends BaseConduitVerticleTest {

    @Test
    public void testUpdateArticle(TestContext tc) {
        tc.assertTrue(true);
        Async async = tc.async();

        webClient.put(8080, "localhost", "/api/articles/test-article-1")
                .putHeader(HttpProps.CONTENT_TYPE, HttpProps.JSON)
                .putHeader(HttpProps.XREQUESTEDWITH, HttpProps.XMLHTTPREQUEST)
                .putHeader(HttpProps.AUTHORIZATION, TestProps.TOKEN_USER1)
                .sendJsonObject(new JsonObject()
                        .put("article", new JsonObject()
                                .put("title", "Test Article 1")
                                .put("description", "Test description 1")
                                .put("body", "Test body.")
                                .put("tagList", new JsonArray().add("test1").add("test2").add("test3"))
                        ), ar -> {
                    if (ar.succeeded()) {
                        tc.assertEquals(200, ar.result().statusCode());
                        JsonObject returnedJson = ar.result().bodyAsJsonObject();
                        tc.assertNotNull(returnedJson);
                        Article returnedArticle = new Article(returnedJson.getJsonObject("article"));
                        tc.assertEquals("Test Article 1", returnedArticle.getTitle(), "Title should be 'Test Article 1'");
                        tc.assertEquals("Test description 1", returnedArticle.getDescription(), "Description should be 'Test description 1'");
                        tc.assertEquals("Test body.", returnedArticle.getBody(), "Body should be 'Test body'");
                        tc.assertEquals("test-article-1", returnedArticle.getSlug(), "Slug shold be 'test-article-1");
                        tc.assertEquals(3, returnedArticle.getTagsList().size(), "There should be 3 tags");
                        async.complete();
                    }else{
                        tc.fail(ar.cause());
                    }

                });


    }
}

