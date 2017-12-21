package io.vertx.conduit.users;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import io.vertx.conduit.users.models.MongoConstants;
import io.vertx.conduit.users.models.TestUser;
import io.vertx.conduit.users.models.User;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTOptions;
import io.vertx.ext.auth.mongo.MongoAuth;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.starter.MainVerticle;
import org.junit.*;
import org.junit.runner.RunWith;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(VertxUnitRunner.class)
public class ProfileTest {

  private static final String TEST_USER_BIO = "This is my bio.";

  private Vertx vertx;

  private static MongodProcess MONGO;

  private static int MONGO_PORT = 12345;

  private User testUser;

  /**
   * Setup the embedded MongoDB once before any tests are run
   *
   * @throws IOException
   */
  @BeforeClass
  public static void initialize() throws IOException {

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
  public void setUp(TestContext tc) {
    vertx = Vertx.vertx();

    DeploymentOptions options = new DeploymentOptions()
      .setConfig(new JsonObject()
        .put("http.port", 8080)
        .put("db_name", "users")
        .put("connection_string", "mongodb://localhost:" + MONGO_PORT)
      );

    vertx.deployVerticle(MainVerticle.class.getName(), options, tc.asyncAssertSuccess());

    MongoClient mongoClient = io.vertx.ext.mongo.MongoClient.createShared(vertx, new JsonObject().put("db_name", "conduit").put("connection_string", "mongodb://localhost:12345"));

    testUser = new User("username","email@domain.com","password");

    Async async1 = tc.async();
    Async async2 = tc.async();

    MongoAuth loginAuthProvider = MongoAuth.create(mongoClient, new JsonObject());
    loginAuthProvider.setUsernameField("email");

    mongoClient.dropCollection(MongoConstants.COLLECTION_NAME_USERS, r->{
      if (!r.succeeded()) {
        fail("Failure deleting the collection " + MongoConstants.COLLECTION_NAME_USERS);
      }
    });
    mongoClient.dropCollection(MongoAuth.DEFAULT_COLLECTION_NAME, r->{
      if (!r.succeeded()) {
        fail("Failure deleting the collection " + MongoAuth.DEFAULT_COLLECTION_NAME);
      }
    });

    loginAuthProvider.insertUser(testUser.getEmail(), testUser.getPassword(), null, null, res ->{
      if (res.succeeded()) {
        mongoClient.insert(MongoConstants.COLLECTION_NAME_USERS, testUser.toMongoJson(), r-> {
          if (r.succeeded()) {
            async2.complete();
          }else{
            fail("Error inserting test user");
          }
        });
        async1.complete();
      }else{
        async1.complete();
        fail();
      }
    });

    JWTAuth jwtAuth = JWTAuth.create(vertx, new JsonObject().put("keyStore", new JsonObject()
      .put("type", "jceks")
      .put("path", "keystore.jceks")
      .put("password", "secret")));

    testUser.setToken(jwtAuth.generateToken(new JsonObject().put("sub", testUser.getEmail()), new JWTOptions()));

  }
  @After
  public void tearDown(TestContext tc) {
    vertx.close(tc.asyncAssertSuccess());
  }

  @AfterClass
  public static void shutdown() {  MONGO.stop(); }

  @Test
  public void testUpdatingUser(TestContext testContext) {
    Async async = testContext.async();

    JsonObject json = new TestUser("email@domain.com", "password").toLoginJson();
    final String length = String.valueOf(json.toString().length());

/*
    // Log our user in so that we can get a JWT Token
    vertx.createHttpClient(new HttpClientOptions().setSsl(true).setTrustAll(true))
      .post(8080, "localhost", "/api/users/login")
      .putHeader("content-type", "application/json")
      .putHeader("content-length", length)
      .handler(response -> {
        testContext.assertEquals(response.statusCode(), 200);
        testContext.assertTrue(response.headers().get("content-type").contains("application/json"));
        response.bodyHandler(body -> {
          testUser = new User(body.toJsonObject().getJsonObject("user"));
          async.complete();
        });
      }).write(json.toString()).end();
*/

    // update the profile
    testUser.setBio(TEST_USER_BIO);
    vertx.createHttpClient(new HttpClientOptions().setSsl(true).setTrustAll(true))
      .post(8080, "localhost", "/api/user")
      .putHeader("content-type", "application/json")
      .putHeader("content-length", length)
      .putHeader("Authentication", "Token " + testUser.getToken())
      .handler(response -> {
        testContext.assertEquals(response.statusCode(), 200);
        testContext.assertTrue(response.headers().get("content-type").contains("application/json"));
        response.bodyHandler(body -> {
          User userResult = new User(body.toJsonObject().getJsonObject("user"));
          assertEquals(TEST_USER_BIO, userResult.getBio());
          async.complete();
        });
      }).write(testUser.toJson().toString());
  }

  @Test
  public void testJWTAuthenticationForUpdateProfile(TestContext testContext) {

    Async async = testContext.async();

    // update the profile
    testUser = new User("username", "email@domain.com", "password");
    testUser.setBio(TEST_USER_BIO);

    vertx.createHttpClient(new HttpClientOptions().setSsl(true).setTrustAll(true))
      .post(8080, "localhost", "/api/user")
      .putHeader("content-type", "application/json")
      .putHeader("content-length", String.valueOf(testUser.toJson().toString().length()))
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
