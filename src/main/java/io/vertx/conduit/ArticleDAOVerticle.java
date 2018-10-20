package io.vertx.conduit;


import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.function.Consumer;

import io.vertx.conduit.errors.RegistrationError;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class ArticleDAOVerticle extends AbstractVerticle{

	
	private static final String MONGO_EXAMPLES_DIR = "mongo-examples";
	  private static final String MONGO_EXAMPLES_JAVA_DIR = MONGO_EXAMPLES_DIR + "/src/main/java/";
	  private static final String MONGO_EXAMPLES_JS_DIR = MONGO_EXAMPLES_DIR + "/src/main/js/";
	  
	  private static final String URI = "mongodb://127.0.0.1:27017";
	  private static final String DB="mydb";
	
	  
	  
	  
	  
	  
	  @Override
	  public void start(Future<Void> startFuture) throws Exception {
		  
		  
		 // JsonObject config = Vertx.currentContext().config();
		  
		  
		  
		  Router apiRouter = Router.router(vertx);
		  apiRouter.route("/api/*").handler(BodyHandler.create());
		  apiRouter.get("/api/articles").handler(this::getArticles);
		  apiRouter.post("/api/articles").handler(this::saveArticle);
		  apiRouter.delete("/api/articles/:slug").handler(this::deleteArticle);
		  apiRouter.put("/api/articles/:slug").handler(this::updateArticle);
		   
		  
		  
		  
		  vertx.createHttpServer()
	      .requestHandler(apiRouter::accept)
	      .listen(8080, result -> {
	        if (result.succeeded()) {
	          startFuture.complete();
	        } else {
	          startFuture.fail(result.cause());
	        }
	      });
		  
		  
		  System.out.println("started in start ");
		  
		  
		  
		  

		 
		  
	  }
	  
	  
	  
	  private void saveArticle(RoutingContext routingContext)
	  {
		  
		 System.out.println("encodebody="+ routingContext.getBodyAsString());
		  
		  
		  
		  JsonObject mongoconfig = new JsonObject()
			        .put("connection_string", URI)
			        .put("db_name", DB);
		  MongoClient mongoClient = MongoClient.createShared(vertx, mongoconfig);
		  
		  
		  mongoClient.save("article", routingContext.getBodyAsJson(), id -> {
		        
		        if(id.succeeded())
		        {
		        	System.out.println("insert success=");
		        	
		        	routingContext.response()
		             .setStatusCode(200)
		             .putHeader("content-type", "application/json; charset=utf-8")
		             .end(new JsonObject().put("response", "Success").encodePrettily());
		        }
		        else
		        {
		        	routingContext.response()
		             .setStatusCode(200)
		             .putHeader("content-type", "application/json; charset=utf-8")
		             .end("Insert failure "+id.cause());
		        }
		  });
	  }
	  private void getArticles(RoutingContext routingContext) {
		  
		  
		  
		  JsonObject mongoconfig = new JsonObject()
			        .put("connection_string", URI)
			        .put("db_name", DB);
		  
		  MongoClient mongoClient = MongoClient.createShared(vertx, mongoconfig);
		  System.out.println("after mongoclient " );
		  
		    mongoClient.find("article", new JsonObject().put("article.createdby", "system"), res -> {
		    	
		        if(res.succeeded())
		        {
		        	 System.out.println("succeeded get query" );
		        	 System.out.println("Object is " + res.result().get(0).encodePrettily());
		        	 
		        	 
		        	 routingContext.response()
		             .setStatusCode(200)
		             .putHeader("content-type", "application/json; charset=utf-8")
		             .end(res.result().get(0).encodePrettily());
		        	 
		        	 
		        	 
		        	 
		        }
		        else
		        {
		        	System.out.println("message eror=" +res.cause().getMessage());
		        	
		        	
		        	 routingContext.response()
		             .setStatusCode(400)
		             .putHeader("content-type", "application/json; charset=utf-8")
		             .end(new JsonObject().put("response", "get failure").encodePrettily());
		        	
		        	
		        }
		        
		    });
		            
		    
		    
		    
		    
		    
		  
		  
		  
		  
	  }
	  private void updateArticle(RoutingContext routingContext)
	  {
		  JsonObject mongoconfig = new JsonObject()
			        .put("connection_string", URI)
			        .put("db_name", DB);
		  
		  MongoClient mongoClient = MongoClient.createShared(vertx, mongoconfig);
		  System.out.println("after mongoclient " );
		  
		  
		  System.out.println("encodebody="+ routingContext.getBodyAsString());
		  
		  JsonObject query=new JsonObject().put("article.title",routingContext.request().getParam("slug"));
		  JsonObject updateQuery=new JsonObject().put("$set",routingContext.getBodyAsJson());
		  
		  
		  
		  
		  mongoClient.update("article", query,updateQuery, rs -> {
              
			  if (rs.succeeded()) {
            	  routingContext.response()
		             .setStatusCode(200)
		             .putHeader("content-type", "application/json; charset=utf-8")
		             .end(new JsonObject().put("response", "Article removed successfully").encodePrettily());
              }
              else
              {
            	  routingContext.response()
		             .setStatusCode(400)
		             .putHeader("content-type", "application/json; charset=utf-8")
		             .end(new JsonObject().put("response", "error removing article").encodePrettily());
              }
            });
	  }
	  
	  private void deleteArticle(RoutingContext routingContext)
	  {
		  JsonObject mongoconfig = new JsonObject()
			        .put("connection_string", URI)
			        .put("db_name", DB);
		  
		  MongoClient mongoClient = MongoClient.createShared(vertx, mongoconfig);
		  System.out.println("after mongoclient " );
		  
		  
		  System.out.println("encodebody="+ routingContext.getBodyAsString());
		  
		  
		  mongoClient.remove("article", new JsonObject().put("article.title",routingContext.request().getParam("slug")), rs -> {
              
			  if (rs.succeeded()) {
            	  routingContext.response()
		             .setStatusCode(200)
		             .putHeader("content-type", "application/json; charset=utf-8")
		             .end(new JsonObject().put("response", "Article removed successfully").encodePrettily());
              }
              else
              {
            	  routingContext.response()
		             .setStatusCode(400)
		             .putHeader("content-type", "application/json; charset=utf-8")
		             .end(new JsonObject().put("response", "error removing article").encodePrettily());
              }
            });
	  }
	  
	  private JsonObject dummyArticleObject()
	  {
		  JsonObject author =new JsonObject();
		  JsonObject articleDetails=new JsonObject();
		 
		  
		  articleDetails.put("slug", "how-to-train-your-dragon");
		  articleDetails.put("title", "how to train your dragon");
		  articleDetails.put("description", "Ever wonder how?");
		  articleDetails.put("body", "It takes a Jacobian");
		  articleDetails.put("tagList", new JsonArray().add("dragons").add("training"));
		  articleDetails.put("createdAt",new Timestamp(System.currentTimeMillis()).toString());
		  articleDetails.put("updatedAt",new Timestamp(System.currentTimeMillis()).toString());
		  articleDetails.put("favorited",false);
		  articleDetails.put("favoritesCount",0);
		  articleDetails.put("author", author);
		  
		  
		  author.put("username", "jake");
		  author.put( "bio", "I work at statefarm");
		  author.put( "image","https://i.stack.imgur.com/xHWG8.jpg");
		  author.put("following", false);
		  author.put("createdby", "system");
		  
		  
		  
		  
		  
		  return articleDetails;
		  
	  }
	  
	  
	  
	  
	  
	  
	  
	  
	  
	  
	  
	  
	  
	  
	  
	  
	  
	  
	
	
	 
	
}
