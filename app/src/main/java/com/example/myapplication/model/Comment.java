package com.example.myapplication.model;

import com.example.myapplication.BaseEntity;
import java.util.Map;

public class Comment extends BaseEntity {

    private String postId;
    private String authorId;
    private String content;
    private String username;
    private String avatarUrl;

    public Comment() {
        super();
    }

    public Comment(String postId, String authorId, String content) {
        super();
        this.postId = postId;
        this.authorId = authorId;
        this.content = content != null ? content.trim() : null;
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
        touch();
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
        touch();
    }

    public String getUserId() {
        return authorId;
    }

    public void setUserId(String userId) {
        this.authorId = userId;
        touch();
    }

    public String getUsername() {
        return username != null && !username.isEmpty() ? username : authorId;
    }

    public void setUsername(String username) {
        this.username = username;
        touch();
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
        touch();
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content != null ? content.trim() : null;
        touch();
    }

    public boolean isOwnedBy(String userId) {
        return authorId != null && userId != null && authorId.equals(userId);
    }

    @Override
    public boolean validate() {
        return postId != null && !postId.trim().isEmpty()
                && authorId != null && !authorId.trim().isEmpty()
                && content != null && !content.trim().isEmpty();
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = baseMap();
        map.put("postId", postId);
        map.put("authorId", authorId);
        map.put("content", content);
        if (username != null) map.put("username", username);
        if (avatarUrl != null) map.put("avatarUrl", avatarUrl);
        return map;
    }

    public static Comment fromMap(Map<String, Object> map) {
        if (map == null) return null;
        Comment comment = new Comment();
        if (map.containsKey("id") && map.get("id") != null) comment.setId((String) map.get("id"));
        if (map.containsKey("postId") && map.get("postId") != null) comment.setPostId((String) map.get("postId"));
        if (map.containsKey("authorId") && map.get("authorId") != null) comment.setAuthorId((String) map.get("authorId"));
        if (map.containsKey("content") && map.get("content") != null) comment.setContent((String) map.get("content"));
        if (map.containsKey("username") && map.get("username") != null) comment.setUsername((String) map.get("username"));
        if (map.containsKey("avatarUrl") && map.get("avatarUrl") != null) comment.setAvatarUrl((String) map.get("avatarUrl"));
        return comment;
    }
}
