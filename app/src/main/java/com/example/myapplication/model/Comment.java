package com.example.myapplication.model;

import com.example.myapplication.BaseEntity;
import java.util.Map;

public class Comment extends BaseEntity {

    private String postId;
    private String authorId;
    private String username;
    private String avatarUrl;
    private String content;
    private boolean hidden;

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

    public String getUsername() { return username; }

    public void setUsername(String username) { this.username = username; }

    public String getAvatarUrl() { return avatarUrl; }

    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getContent() {
        return content;
    }

    public boolean isHidden() { return hidden; }
    public void setHidden(boolean hidden) { this.hidden = hidden; }

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
        map.put("username", username);
        map.put("avatarUrl", avatarUrl);
        map.put("content", content);
        map.put("hidden", hidden);
        return map;
    }
}
