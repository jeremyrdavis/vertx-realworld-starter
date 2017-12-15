package io.vertx.conduit.users;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.*;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import io.vertx.conduit.errors.ErrorMessages;
import io.vertx.conduit.errors.LoginError;
import io.vertx.conduit.users.models.MongoConstants;
import io.vertx.conduit.users.models.User;
import io.vertx.conduit.users.models.TestUser;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.mongo.MongoAuth;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.starter.MainVerticle;
import org.junit.*;
import org.junit.runner.RunWith;

import java.io.IOException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

@RunWith(VertxUnitRunner.class)
public class LoginUserTest {

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

    io.vertx.ext.mongo.MongoClient mongoClient = io.vertx.ext.mongo.MongoClient.createShared(vertx, new JsonObject().put("db_name", "conduit").put("connection_string", "mongodb://localhost:12345"));

    Async async = tc.async();
    MongoAuth loginAuthProvider = MongoAuth.create(mongoClient, new JsonObject());
    loginAuthProvider.setUsernameField("email");
    loginAuthProvider.insertUser("email@domain.com", "password", null, null, res ->{
      if (res.succeeded()) {
        System.out.println("inserted " + res.result());
        async.complete();
      }else{
        async.complete();
        fail();
      }
    });

//    User testUser = new User("username", "email@domain.com", "password");
//    testUser.setBio("I am a new user of conduit");

//    Async async = tc.async();
//    mongoClient.insert(MongoConstants.COLLECTION_NAME_USERS, testUser.toJson(), r->{
//      if (r.succeeded()) {
//        async.complete();
//      }else{
//        fail();
//        async.complete();
//      }
//    });

  }

  @After
  public void tearDown(TestContext tc) {
    vertx.close(tc.asyncAssertSuccess());
  }

  @AfterClass
  public static void shutdown() {  MONGO.stop(); }

  /*
  @Test
  public void webClientTest(TestContext testContext) {

    Async async = testContext.async();

    JsonObject json = new TestUser("email@domain.com", "password").toLoginJson();
    final String length = String.valueOf(json.toString().length());


    HttpClientRequest req = vertx.createHttpClient(new HttpClientOptions()
      .setSsl(true)
      .setTrustAll(true))
      .post(8080, "localhost", "/api/users/login");

    req.handler(res ->{
      testContext.assertTrue(res.statusCode(), 200);
      HttpClientResponse httpClientResponse = res.bodyHandler();
      httpClientResponse.
    });


    WebClient webClient = WebClient.create(vertx);
    webClient.post(8080, "localhost/api/users/login", json.toString()).send(ar ->{
      testContext.assertTrue(ar.succeeded());
      HttpResponse<Buffer> response = ar.result();
      final User conduitUser = Json.decodeValue(response.toString(), User.class);
      System.out.println("Returned value: " + conduitUser.toJson());
      testContext.assertNotNull(conduitUser);
    });

  }
  */

  @Test
  public void testLoggingIn(TestContext testContext) {
    Async async = testContext.async();

    JsonObject json = new TestUser("email@domain.com", "password").toLoginJson();
    final String length = String.valueOf(json.toString().length());

    vertx.createHttpClient(new HttpClientOptions().setSsl(true).setTrustAll(true))
      .post(8080, "localhost", "/api/users/login")
      .putHeader("content-type", "application/json")
      .putHeader("content-length", length)
      .handler(response -> {
        testContext.assertEquals(response.statusCode(), 200);
        testContext.assertTrue(response.headers().get("content-type").contains("application/json"));
        response.bodyHandler(body -> {
          testContext.assertNotNull(body);
          System.out.println("Returned body: " + body.toString());
          final User user = new User(body.toJsonObject().getJsonObject("user"));
          System.out.println("Returned value: " + user.toJson());
          testContext.assertEquals("email@domain.com", user.getEmail());
//          testContext.assertTrue(user.getToken().length() >= 1);
          testContext.assertNotNull(user.get_id());
          async.complete();
        });
      }).write(json.toString()).end();

  }

  @Test
  public void testInvalidLogin(TestContext testContext) {
    Async async = testContext.async();

    JsonObject json = new TestUser("wrongemail@domain.com", "password").toLoginJson();
    final String length = String.valueOf(json.toString().length());

    vertx.createHttpClient(new HttpClientOptions()
      .setSsl(true).setTrustAll(true)).post(8080, "localhost", "/api/users/login")
      .putHeader("content-type", "application/json")
      .putHeader("content-length", length)
      .handler(response -> {
        //make sure that we return the appropriate HTTP response code
        testContext.assertFalse(response.statusCode() == 200);
        testContext.assertEquals(response.statusCode(), 422);
        testContext.assertTrue(response.headers().get("content-type").contains("application/json"));
        response.bodyHandler(body -> {
          testContext.assertNotNull(body);
          final LoginError loginError = Json.decodeValue(body.toString(), LoginError.class);
          System.out.println("Returned value: " + loginError.toJson());
          testContext.assertEquals(loginError.getMessage(), ErrorMessages.LOGIN_ERROR);
        });
        async.complete();
      }).write(json.toString()).end();

  }


}
