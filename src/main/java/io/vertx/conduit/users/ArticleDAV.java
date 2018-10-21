package io.vertx.conduit.users;

import io.vertx.conduit.MessagingErrorCodes;
import io.vertx.conduit.users.models.MongoConstants;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.mongo.MongoClient;

import static io.vertx.conduit.MessagingProps.*;

public class ArticleDAV extends AbstractVerticle {

    public static final String MESSAGE_ARTICLES = "address.articles";
    public static final String MESSAGE_ACTION_LOOKUP_ARTICLE_BY_SLUG = "action.lookup.article.by.slug";
    private static final Logger LOGGER = LoggerFactory.getLogger(ArticleDAV.class);
    private static final String DEFAULT_COLLECTION = "article";

    // for DB access
    private MongoClient mongoClient;

    @Override
    public void start(Future<Void> startFuture) {
        LOGGER.info("ArticleDAV starting with config for " + config().getString("env"));

        // Configure the MongoClient inline.  This should be externalized into a config file
        mongoClient = MongoClient.createShared(vertx, new JsonObject().put("db_name", config().getString("db_name", "conduit")).put("connection_string", config().getString("connection_string", "mongodb://localhost:27017")));

        EventBus eventBus = vertx.eventBus();
        MessageConsumer<JsonObject> consumer = eventBus.consumer(MESSAGE_ARTICLES);

        consumer.handler(message -> {

            String action = message.body().getString(MESSAGE_ACTION);
            LOGGER.info(action);

            switch (action) {
                case LOOKUP_BY_FIELD:
                    lookupByField(message);
                    break;
                case DELETE:
                    delete(message);
                    break;
                case MESSAGE_ACTION_UPDATE:
                    update(message);
                    break;
                default:
                    message.fail(1, "Unkown action: " + message.body());
            }
        });

        startFuture.complete();

    }

    private void update(Message<JsonObject> message) {
        JsonObject updateValues = message.body().getJsonObject(DOCUMENT);
        JsonObject query = new JsonObject();
        query.put(message.body().getString(KEY_FIELD), message.body().getString(KEY_VALUE));
        JsonObject update = new JsonObject()
                .put("$set", updateValues);
        mongoClient.updateCollection(MongoConstants.COLLECTION_NAME_ARTICLES, query, update, res ->{
            if (res.succeeded()) {
                lookupByField(message);
            }else{
                message.fail(MessagingErrorCodes.UPDATE_FAILURE.ordinal(), MessagingErrorCodes.UPDATE_FAILURE.message + res.cause().getMessage());
            }
        });

    }

    private void delete(Message<JsonObject> message) {

        JsonObject query = new JsonObject().put(message.body().getString(MESSAGE_LOOKUP_FIELD), message.body().getString(MESSAGE_LOOKUP_VALUE));
        mongoClient.removeDocument(DEFAULT_COLLECTION, query, res -> {
            if (res.succeeded()) {
                LOGGER.info("delete succeeded: " + res.result());
                message.reply(new JsonObject()
                        .put(MESSAGE_RESPONSE_DETAILS, MESSAGE_SUCCESS));
            } else {
                message.fail(MessagingErrorCodes.NOT_FOUND.ordinal(), MessagingErrorCodes.NOT_FOUND.message + res.cause());
            }
        });
    }

    private void lookupByField(Message<JsonObject> message) {

        JsonObject query = new JsonObject().put(message.body().getString(KEY_FIELD), message.body().getString(KEY_VALUE));
        mongoClient.find(DEFAULT_COLLECTION, query, res -> {
            if (res.succeeded()) {
                LOGGER.info("lookup succeeded: " + res.result());
                message.reply(new JsonObject()
                        .put(MESSAGE_RESPONSE_DETAILS, res.result().get(0)));
            } else {
                message.fail(MessagingErrorCodes.NOT_FOUND.ordinal(), MessagingErrorCodes.NOT_FOUND.message + res.cause());
            }
        });
    }


}
