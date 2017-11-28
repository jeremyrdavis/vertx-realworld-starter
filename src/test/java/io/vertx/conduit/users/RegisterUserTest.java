package io.vertx.conduit.users;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import io.vertx.conduit.users.models.User;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.starter.MainVerticle;
import org.junit.*;
import org.junit.runner.RunWith;

import java.io.IOException;

@RunWith(VertxUnitRunner.class)
public class RegisterUserTest {

  private Vertx vertx;

  // MongoDB stuff
  private static MongodProcess MONGO;

  private static int MONGO_PORT = 12345;

  /**
   * Setup the embedded MongoDB once before any tests are run
   *
   * @throws IOException
   */
  @BeforeClass
  public static void initialize() throws IOException {
    MongodStarter starter = MongodStarter.getDefaultInstance();
    IMongodConfig mongodConfig = new MongodConfigBuilder()
      .version(Version.Main.PRODUCTION)
      .net(new Net("localhost", MONGO_PORT, Network.localhostIsIPv6()))
      .build();
    MongodExecutable mongodExecutable = starter.prepare(mongodConfig);
    MONGO = mongodExecutable.start();
  }


  @Before
  public void setUp(TestContext tc) {
    vertx = Vertx.vertx();

    DeploymentOptions options = new DeploymentOptions()
      .setConfig(new JsonObject()
        .put("http.port", 8080)
        .put("db_name", "users")
        .put("connection_string", "mongodb://localhost:" + MONGO_PORT)
      );

    vertx.deployVerticle(MainVerticle.class.getName(), options, tc.asyncAssertSuccess());
  }

  @After
  public void tearDown(TestContext tc) {
    vertx.close(tc.asyncAssertSuccess());
  }

  @AfterClass
  public static void shutdown() {  MONGO.stop(); }

  @Test
  public void testRegisteringAUser(TestContext tc) {
    Async async = tc.async();

    final String json = Json.encodePrettily(new User("username", "user@domain.com", "password"));
    final String length = String.valueOf(json.length());

    vertx.createHttpClient(new HttpClientOptions()
      .setSsl(true).setTrustAll(true)).post(8080, "localhost", "/api/users")
      .putHeader("content-type", "application/json")
      .putHeader("content-length", length)
      .handler(response -> {
          tc.assertEquals(response.statusCode(), 201);
          tc.assertTrue(response.headers().get("content-type").contains("application/json"));
          response.bodyHandler(body -> {
            final User user = Json.decodeValue(body.toString(), User.class);
            tc.assertEquals(user.getUsername(), "username");
            tc.assertEquals(user.getEmail(), "user@domain.com");
            tc.assertNotNull(user.getId());
            async.complete();
          });
      }).write(json).end();
  }

}
