package com.example.myapplication.model;

import com.example.myapplication.BaseEntity;
import java.util.Map;

public class Post extends BaseEntity {

    private String authorId;
    private String imageUrl;
    private String caption;
    private long likesCount;
    private long commentsCount;

    // Additional fields for convenient UI rendering
    private String authorUsername;
    private String authorAvatarUrl;

    public Post() {
        super();
    }

    public Post(String id, String authorId, String imageUrl, String caption, long likesCount, long commentsCount) {
        super(id);
        this.authorId = authorId;
        this.imageUrl = imageUrl;
        this.caption = caption;
        this.likesCount = likesCount;
        this.commentsCount = commentsCount;
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public long getLikesCount() {
        return likesCount;
    }

    public void setLikesCount(long likesCount) {
        this.likesCount = likesCount;
    }

    public long getCommentsCount() {
        return commentsCount;
    }

    public void setCommentsCount(long commentsCount) {
        this.commentsCount = commentsCount;
    }

    public String getAuthorUsername() {
        return authorUsername;
    }

    public void setAuthorUsername(String authorUsername) {
        this.authorUsername = authorUsername;
    }

    public String getAuthorAvatarUrl() {
        return authorAvatarUrl;
    }

    public void setAuthorAvatarUrl(String authorAvatarUrl) {
        this.authorAvatarUrl = authorAvatarUrl;
    }

    @Override
    public boolean validate() {
        return authorId != null && !authorId.trim().isEmpty()
                && imageUrl != null && !imageUrl.trim().isEmpty();
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = baseMap();
        map.put("authorId", authorId);
        map.put("imageUrl", imageUrl);
        map.put("caption", caption != null ? caption : "");
        map.put("likesCount", likesCount);
        map.put("commentsCount", commentsCount);
        if (authorUsername != null) map.put("authorUsername", authorUsername);
        if (authorAvatarUrl != null) map.put("authorAvatarUrl", authorAvatarUrl);
        return map;
    }

    public static Post fromMap(Map<String, Object> map) {
        if (map == null) return null;
        Post post = new Post();
        if (map.containsKey("id") && map.get("id") != null) post.setId((String) map.get("id"));
        if (map.containsKey("authorId") && map.get("authorId") != null) post.setAuthorId((String) map.get("authorId"));
        if (map.containsKey("imageUrl") && map.get("imageUrl") != null) post.setImageUrl((String) map.get("imageUrl"));
        if (map.containsKey("caption") && map.get("caption") != null) post.setCaption((String) map.get("caption"));
        if (map.containsKey("likesCount") && map.get("likesCount") instanceof Number) {
            post.setLikesCount(((Number) map.get("likesCount")).longValue());
        }
        if (map.containsKey("commentsCount") && map.get("commentsCount") instanceof Number) {
            post.setCommentsCount(((Number) map.get("commentsCount")).longValue());
        }
        if (map.containsKey("authorUsername") && map.get("authorUsername") != null) {
            post.setAuthorUsername((String) map.get("authorUsername"));
        }
        if (map.containsKey("authorAvatarUrl") && map.get("authorAvatarUrl") != null) {
            post.setAuthorAvatarUrl((String) map.get("authorAvatarUrl"));
        }
        return post;
    }
}
