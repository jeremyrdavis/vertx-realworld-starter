package io.vertx.conduit.tests.eventbus;

import io.vertx.conduit.Article;
import io.vertx.conduit.BaseConduitVerticleTest;
import io.vertx.conduit.DBSetupVerticle;
import io.vertx.conduit.users.ArticleDAV;
import io.vertx.conduit.users.models.User;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

import static io.vertx.conduit.MessagingProps.*;
import static io.vertx.conduit.TestProps.DB_CONNECTION_STRING_TEST;
import static io.vertx.conduit.TestProps.DB_NAME_TEST;
import static io.vertx.conduit.users.ArticleDAV.MESSAGE_ARTICLES;

@RunWith(VertxUnitRunner.class)
public class UpdateArticleEventBusTest extends BaseConduitVerticleTest{

    User jacob;

    Article testArticle1;

    @Before
    public void setUp(TestContext tc) {

        vertx = Vertx.vertx();

        DeploymentOptions options = new DeploymentOptions()
                .setConfig(new JsonObject()
                        .put("http.port", 8080)
                        .put("db_name", DB_NAME_TEST)
                        .put("connection_string", DB_CONNECTION_STRING_TEST)
                );

        jacob = new User("jake@jake.jake", "jakejake", "Jacob", "I work at state farm", null);
        testArticle1 = new Article("Test Article 1", "New Test description 1", "Lorem ipsum dolor site amet.", new ArrayList<String>(3){ { add("test1"); add("test2"); add("test3"); } });
        testArticle1.setSlug("test-article-1");

        vertx.deployVerticle(DBSetupVerticle.class.getName(), tc.asyncAssertSuccess());
        vertx.deployVerticle(ArticleDAV.class.getName(), options, tc.asyncAssertSuccess());
    }

    @Test
    public void testUpdateArticle(TestContext testContext) {

        Async async = testContext.async();

        JsonObject update = new JsonObject()
                .put("title", testArticle1.getTitle())
                .put("description", testArticle1.getDescription())
                .put("body", testArticle1.getBody());

        JsonObject message = new JsonObject()
                .put(MESSAGE_ACTION, MESSAGE_ACTION_UPDATE)
                .put(KEY_FIELD, "slug")
                .put(KEY_VALUE, testArticle1.getSlug())
                .put(DOCUMENT, update);

        vertx.<JsonObject>eventBus().send(MESSAGE_ARTICLES, message, ar -> {
            testContext.assertTrue(ar.succeeded());
            JsonObject result = (JsonObject) ar.result().body();

            Article returnedArticle = new Article(result.getJsonObject(MESSAGE_RESPONSE_DETAILS));
            testContext.assertEquals("Test Article 1", returnedArticle.getTitle(), "Title should be 'Test Article 1'");
            testContext.assertEquals("New Test description 1", returnedArticle.getDescription(), "Description should be 'Test description 1'");
            testContext.assertEquals("Lorem ipsum dolor site amet.", returnedArticle.getBody(), "Body should be 'Lorem ipsum dolor site amet.'");
            testContext.assertNotNull("There should be a slug", returnedArticle.getSlug());
            testContext.assertEquals("test-article-1", returnedArticle.getSlug(), "Slug shold be 'test-article-1");

            async.complete();
        });
    }
}
