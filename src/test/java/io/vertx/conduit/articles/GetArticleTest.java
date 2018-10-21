package io.vertx.conduit.articles;

import io.vertx.conduit.Article;
import io.vertx.conduit.BaseConduitVerticleTest;
import io.vertx.conduit.HttpProps;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

@RunWith(VertxUnitRunner.class)
public class GetArticleTest extends BaseConduitVerticleTest{

    @Test
    public void testGetArticle(TestContext tc) {
        Async async = tc.async();

        webClient.get(8080, "localhost", "/api/articles/test-article-1")
                .putHeader(HttpProps.CONTENT_TYPE, HttpProps.JSON)
                .putHeader(HttpProps.XREQUESTEDWITH, HttpProps.XMLHTTPREQUEST)
                .send(ar -> {
                    if (ar.succeeded()) {
                        tc.assertEquals(200, ar.result().statusCode());
                        JsonObject returnedJson = ar.result().bodyAsJsonObject();
                        tc.assertNotNull(returnedJson);
                        Article returnedArticle = new Article(returnedJson.getJsonObject("article"));

                        testArticle1 = new Article("Test Article 1", "Test description 1", "Lorem ipsum dolor site amet.", new ArrayList<String>(3){ { add("test1"); add("test2"); add("test3"); } });

                        tc.assertEquals("Test Article 1", returnedArticle.getTitle(), "Title should be 'Test Article 1'");
                        tc.assertEquals("Test description 1", returnedArticle.getDescription(), "Description should be 'Test description 1'");
                        tc.assertEquals("Lorem ipsum dolor site amet.", returnedArticle.getBody(), "Body should be 'Lorem ipsum dolor site amet.'");
                        tc.assertNotNull("There should be a slug", returnedArticle.getSlug());
                        tc.assertEquals("test-article-1", returnedArticle.getSlug(), "Slug shold be 'test-article-1");
                        tc.assertNotNull(returnedArticle.getTagsList(), "TagsList should not be null");
                        tc.assertEquals(3, returnedArticle.getTagsList().size(), "There should be 3 tags");
                        async.complete();
                    }else{
                        tc.fail(ar.cause());
                    }

                });


    }
}
