package io.vertx.conduit;

import io.vertx.core.Vertx;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class BaseVerticleTest {

  private Vertx vertx;

  @Test
  public void testThatTheServerIsStarted(TestContext tc) {

    vertx = Vertx.vertx();
    vertx.deployVerticle(new DBSetupVerticle(), tc.asyncAssertSuccess());
  }


}
