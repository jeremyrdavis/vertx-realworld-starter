package io.vertx.conduit.users;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.*;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import io.vertx.conduit.errors.ErrorMessages;
import io.vertx.conduit.errors.LoginError;
import io.vertx.conduit.users.models.MongConstants;
import io.vertx.conduit.users.models.TestUser;
import io.vertx.conduit.users.models.User;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.mongo.MongoAuth;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.starter.MainVerticle;
import org.bson.Document;
import org.junit.*;
import org.junit.runner.RunWith;

import java.io.IOException;

import static org.junit.Assert.assertNull;

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

    User user = new User("username", "email@domain.com", "password");

    IMongoImportConfig mongoImportConfig = new MongoImportConfigBuilder()
      .version(Version.Main.PRODUCTION)
      .net(new Net("localhost", MONGO_PORT, Network.localhostIsIPv6()))
      .db(MongConstants.DB_NAME.value)
      .collection(MongConstants.COLLECTION_NAME_USERS.value)
      .upsert(true)
      .dropCollection(true)
      .jsonArray(false)
      .importFile(user.toJson().toString())
      .build();

    try {
      MongoClient mongo = new MongoClient("localhost", MONGO_PORT);
      MongoDatabase database = mongo.getDatabase(MongConstants.DB_NAME.value);
      MongoCollection<Document> collection = database.getCollection(MongoAuth.DEFAULT_COLLECTION_NAME);

      // add an index so that the username field is unique
      collection.createIndex(Indexes.text("email"), new IndexOptions().unique(true));
      Document document = new Document("username", "username").append("email", "email@domain.com").append("password", "password");
      collection.insertOne(document);
    } catch (Exception e) {
      assertNull("There should not be an error when pre-populating the database", e);
    }

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
  public void testLoggingIn(TestContext testContext) {
    Async async = testContext.async();

    JsonObject json = new TestUser("email@domain.com", "password").toLoginJson();
    final String length = String.valueOf(json.toString().length());

    vertx.createHttpClient(new HttpClientOptions()
      .setSsl(true).setTrustAll(true)).post(8080, "localhost", "/api/users/login")
      .putHeader("content-type", "application/json")
      .putHeader("content-length", length)
      .handler(response -> {
        testContext.assertEquals(response.statusCode(), 200);
        testContext.assertTrue(response.headers().get("content-type").contains("application/json"));
        response.bodyHandler(body -> {
          testContext.assertNotNull(body);
          final User user = Json.decodeValue(body.toString(), User.class);
          System.out.println("Returned value: " + user.toJson());
          testContext.assertEquals(user.getUsername(), "username");
          testContext.assertEquals(user.getEmail(), "user@domain.com");
          testContext.assertNotNull(user.get_id());
        });
        async.complete();
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
          testContext.assertEquals(loginError.getMessage(), ErrorMessages.LOGIN_ERROR.message);
        });
        async.complete();
      }).write(json.toString()).end();

  }


}
