package io.vertx.conduit.articles;

import io.vertx.conduit.BaseConduitVerticleTest;
import io.vertx.conduit.HttpProps;
import io.vertx.conduit.TestProps;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class DeleteArticleTest extends BaseConduitVerticleTest {

    @Test
    public void testDeleteArticle(TestContext tc) {
        tc.assertTrue(true);
        Async async = tc.async();

        webClient.delete(8080, "localhost", "/api/articles/test-article-1")
                .putHeader(HttpProps.CONTENT_TYPE, HttpProps.JSON)
                .putHeader(HttpProps.XREQUESTEDWITH, HttpProps.XMLHTTPREQUEST)
                .putHeader(HttpProps.AUTHORIZATION, TestProps.TOKEN_JACOB)
                .send(ar -> {
                    if (ar.succeeded()) {
                        tc.assertEquals(200, ar.result().statusCode());
                        async.complete();
                    } else {
                        tc.fail(ar.cause());
                    }
                });
    }

}
