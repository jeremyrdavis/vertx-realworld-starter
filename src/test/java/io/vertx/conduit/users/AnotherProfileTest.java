package io.vertx.conduit.users.models;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.starter.MainVerticle;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.assertEquals;

@RunWith(VertxUnitRunner.class)
public class AnotherProfileTest {

  private static final String TEST_USER_BIO = "This is my bio.";

  private Vertx vertx;

  private static MongodProcess MONGO;

  private static int MONGO_PORT = 12345;

  private User testUser;

  String validToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpYXQiOjE1MTU3MDM3OTIsImV4cCI6MTUxNTcwNzM5Mn0.46uFDkq_hFALgBzcueNY5EOrcUY6lnvpV6Sx_KD8sDk";


  @BeforeClass
  public static void initialize() throws IOException{
    // Database stuff
    MongodStarter starter = MongodStarter.getDefaultInstance();
    IMongodConfig mongodConfig = new MongodConfigBuilder()
      .version(Version.Main.PRODUCTION)
      .net(new Net("localhost", MONGO_PORT, Network.localhostIsIPv6()))
      .build();
    MongodExecutable mongodExecutable = starter.prepare(mongodConfig);
    MONGO = mongodExecutable.start();

  }

  @Before
  public void setUp(TestContext tc){
    vertx = Vertx.vertx();

    DeploymentOptions options = new DeploymentOptions()
      .setConfig(new JsonObject()
        .put("http.port", 8080)
        .put("db_name", "users")
        .put("connection_string", "mongodb://localhost:" + MONGO_PORT)
      );

    vertx.deployVerticle(MainVerticle.class.getName(), options, tc.asyncAssertSuccess());

    JWTAuth jwtAuth = JWTAuth.create(vertx, new JsonObject().put("keyStore", new JsonObject()
      .put("type", "jceks")
      .put("path", "keystore.jceks")
      .put("password", "secret")));
    Calendar cal = Calendar.getInstance();
    cal.add(Calendar.DAY_OF_YEAR, 7);

    testUser = new User("username","email@domain.com","password");

    validToken = jwtAuth.generateToken(new JsonObject().put("sub", testUser.getEmail()).put("exp", cal.getTimeInMillis()), new JWTOptions());

  }

  @Test
  public void testAuthenticationWorksWhenUpdatingBio(TestContext testContext) {
    Async async = testContext.async();

    // update the profile
    testUser = new User("username", "email@domain.com", "password");
    testUser.setBio(TEST_USER_BIO);

    vertx.createHttpClient(new HttpClientOptions().setSsl(true).setTrustAll(true))
      .post(8080, "localhost", "/api/user")
      .putHeader("content-type", "application/json")
      .putHeader("content-length", String.valueOf(testUser.toJson().toString().length()))
      .putHeader("Authorization", "Bearer " + validToken)
      .handler(response -> {
        testContext.assertEquals(response.statusCode(), 401);
        testContext.assertTrue(response.headers().get("content-type").contains("application/json"));
        response.bodyHandler(body -> {
          User userResult = new User(body.toJsonObject().getJsonObject("user"));
          assertEquals(TEST_USER_BIO, userResult.getBio());
          async.complete();
        });
      }).write(testUser.toJson().toString());

  }

  @Test
  public void testAuthenticationKeepsOutInvalidUpdates(TestContext testContext) {
    Async async = testContext.async();

    // update the profile
    testUser = new User("username", "email@domain.com", "password");
    testUser.setBio(TEST_USER_BIO);

    vertx.createHttpClient(new HttpClientOptions().setSsl(true).setTrustAll(true))
      .post(8080, "localhost", "/api/user")
      .putHeader("content-type", "application/json")
      .putHeader("content-length", String.valueOf(testUser.toJson().toString().length()))
      .putHeader("Authorization", "Bearer foo")
      .handler(response -> {
        testContext.assertEquals(response.statusCode(), 401);
        testContext.assertTrue(response.headers().get("content-type").contains("application/json"));
        response.bodyHandler(body -> {
          User userResult = new User(body.toJsonObject().getJsonObject("user"));
          assertEquals(TEST_USER_BIO, userResult.getBio());
          async.complete();
        });
      }).write(testUser.toJson().toString());

  }
}
