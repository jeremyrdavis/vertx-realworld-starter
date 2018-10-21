package io.vertx.conduit;

import io.vertx.conduit.users.models.User;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class Article implements ConduitDomainModel{

    private String id;

    private String slug;

    private String title;

    private String description;

    private String body;

    private List<String> tagsList;

    private Date createdAt;

    private Date updatedAt;

    private boolean favorited;

    private int favoritesCount;

    private User author;

    /**
     * Constructor matching the API for creating an Article
     *
     * @param title
     * @param description
     * @param body
     * @param tagsList
     */
    public Article(String title, String description, String body, List<String> tagsList) {
        this.title = title;
        this.description = description;
        this.body = body;
        this.tagsList = tagsList;
    }

    public Article(JsonObject jsonObject) {
        if (jsonObject.containsKey("_id")) this.id = jsonObject.getString("_id");
        this.slug = jsonObject.getString("slug");
        this.title = jsonObject.getString("title");
        this.description = jsonObject.getString("description");
        this.body = jsonObject.getString("body");
        if (jsonObject.containsKey("tagList")){
            this.tagsList = jsonObject.getJsonArray("tagList").getList();
        }
        if(jsonObject.containsKey("createdAt")) this.createdAt = new Date(jsonObject.getLong("createdAt"));
        if(jsonObject.containsKey("updatedAt")) this.updatedAt = new Date(jsonObject.getLong("updatedAt"));
        if(jsonObject.containsKey("favorited")) this.favorited = jsonObject.getBoolean("favorited");
        if(jsonObject.containsKey("favoritesCount")) this.favoritesCount = jsonObject.getInteger("favoritesCount");
    }

    public Article() {
    }

    public Article(String id, String slug, String title, String description, String body, List<String> tagsList, Date createdAt, Date updatedAt, boolean favorited, int favoritesCount, User author) {
        this.id = id;
        this.slug = slug;
        this.title = title;
        this.description = description;
        this.body = body;
        this.tagsList = tagsList;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.favorited = favorited;
        this.favoritesCount = favoritesCount;
        this.author = author;
    }

    public Article(String slug, String title, String description, String body, List<String> tagsList, Date createdAt, Date updatedAt, boolean favorited, int favoritesCount, User author) {
        this.slug = slug;
        this.title = title;
        this.description = description;
        this.body = body;
        this.tagsList = tagsList;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.favorited = favorited;
        this.favoritesCount = favoritesCount;
        this.author = author;
    }

    public JsonObject toJson(){
        JsonObject retVal = new JsonObject()
            .put("slug", this.slug)
            .put("title", this.title)
            .put("description", this.description)
            .put("body", this.body)
            .put("tagList", this.tagsList)
            .put("favorited", this.favorited)
            .put("favoritesCount", this.favoritesCount);

        if (this.createdAt != null) {
            retVal.put("createdAt", this.createdAt.getTime());
        }else{
            retVal.put("createdAt", this.createdAt);
        }
        if (this.updatedAt != null) {
            retVal.put("updatedAt", this.updatedAt.getTime());
        }else{
            retVal.put("updatedAt", this.updatedAt);
        }
        if (this.author != null) {
            retVal.put("author", this.author.toJson());
        }
        return retVal;
    }

    public JsonObject toConduitJson() {
        JsonObject article = new JsonObject()
            .put("id", this.id)
            .put("slug", this.slug)
                .put("title", this.title)
                .put("description", this.description)
                .put("body", this.body)
                .put("tagList", this.tagsList)
                .put("favorited", this.favorited)
                .put("favoritesCount", this.favoritesCount);
        if (this.createdAt != null) {
            article.put("createdAt", this.createdAt.getTime());
        }else{
            article.put("createdAt", this.createdAt);
        }
        if (this.updatedAt != null) {
            article.put("updatedAt", this.updatedAt.getTime());
        }else{
            article.put("updatedAt", this.updatedAt);
        }
        if (this.author != null) {
            article.put("author", this.author.toJson());
        }
        JsonObject retVal = new JsonObject();
        retVal.put("article", article);
        System.out.println(retVal.toString());
        return retVal;
    }

    @Override
    public JsonObject toMongoJson() {
        JsonObject article = new JsonObject()
                .put("id", this.id)
                .put("slug", this.slug)
                .put("title", this.title)
                .put("description", this.description)
                .put("body", this.body)
                .put("tagList", this.tagsList);
        if (this.createdAt != null) {
            article.put("createdAt", this.createdAt.getTime());
        }else{
            article.put("createdAt", this.createdAt);
        }
        if (this.updatedAt != null) {
            article.put("updatedAt", this.updatedAt.getTime());
        }else{
            article.put("updatedAt", this.updatedAt);
        }
        article.put("favorited", this.favorited)
               .put("favoritesCount", this.favoritesCount);

        if (this.author != null) {
            article.put("author", this.author.get_id());
        }
        return article;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public List<String> getTagsList() {
        return tagsList;
    }

    public void setTagsList(List<String> tagsList) {
        this.tagsList = tagsList;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isFavorited() {
        return favorited;
    }

    public void setFavorited(boolean favorited) {
        this.favorited = favorited;
    }

    public int getFavoritesCount() {
        return favoritesCount;
    }

    public void setFavoritesCount(int favoritesCount) {
        this.favoritesCount = favoritesCount;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }
}
