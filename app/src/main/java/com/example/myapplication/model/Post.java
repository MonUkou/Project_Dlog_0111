package com.example.myapplication.model;

import com.example.myapplication.BaseEntity;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

public class Post extends BaseEntity {

    private String authorId;
    private String imageUrl;
    private List<String> imageUrls = new ArrayList<>();
    private List<String> hashtags = new ArrayList<>();
    private List<Map<String, String>> photoMetadata = new ArrayList<>();
    private String caption;
    private String colorTone;
    private long likesCount;
    private long commentsCount;
    private boolean hidden;

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

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> urls) { imageUrls = urls == null ? new ArrayList<String>() : new ArrayList<>(urls); }
    public List<String> getHashtags() { return hashtags; }
    public void setHashtags(List<String> tags) { hashtags = tags == null ? new ArrayList<String>() : new ArrayList<>(tags); }
    public List<Map<String, String>> getPhotoMetadata() { return photoMetadata; }
    public void setPhotoMetadata(List<Map<String, String>> values) {
        photoMetadata = values == null ? new ArrayList<Map<String, String>>() : new ArrayList<>(values);
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }
    public String getColorTone() { return colorTone; }
    public void setColorTone(String value) { colorTone = value; }

    public long getLikesCount() {
        return likesCount;
    }

    public void setLikesCount(long likesCount) {
        this.likesCount = likesCount;
    }

    public long getCommentsCount() {
        return commentsCount;
    }

    public boolean isHidden() { return hidden; }
    public void setHidden(boolean hidden) { this.hidden = hidden; }

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
        map.put("imageUrls", imageUrls);
        map.put("hashtags", hashtags);
        map.put("photoMetadata", photoMetadata);
        map.put("caption", caption != null ? caption : "");
        if (colorTone != null) map.put("colorTone", colorTone);
        map.put("likesCount", likesCount);
        map.put("commentsCount", commentsCount);
        map.put("hidden", hidden);
        if (authorUsername != null) map.put("authorUsername", authorUsername);
        if (authorAvatarUrl != null) map.put("authorAvatarUrl", authorAvatarUrl);
        return map;
    }

    public static Post fromMap(Map<String, Object> map) {
        if (map == null) return null;
        Post post = new Post();
        post.hidden = Boolean.TRUE.equals(map.get("hidden"));
        if (map.get("createdAt") instanceof Number) post.setCreatedAt(((Number) map.get("createdAt")).longValue());
        if (map.get("updatedAt") instanceof Number) post.setUpdatedAt(((Number) map.get("updatedAt")).longValue());
        if (map.containsKey("id") && map.get("id") != null) post.setId((String) map.get("id"));
        if (map.containsKey("authorId") && map.get("authorId") != null) post.setAuthorId((String) map.get("authorId"));
        if (map.containsKey("imageUrl") && map.get("imageUrl") != null) post.setImageUrl((String) map.get("imageUrl"));
        if (map.get("imageUrls") instanceof List) {
            List<String> urls = new ArrayList<>();
            for (Object value : (List<?>) map.get("imageUrls")) {
                if (value instanceof String && !((String) value).isEmpty()) urls.add((String) value);
            }
            post.setImageUrls(urls);
        }
        if (post.imageUrls.isEmpty() && post.imageUrl != null) post.imageUrls.add(post.imageUrl);
        if (map.get("hashtags") instanceof List) {
            List<String> tags = new ArrayList<>();
            for (Object value : (List<?>) map.get("hashtags")) {
                if (value instanceof String) tags.add((String) value);
            }
            post.setHashtags(tags);
        }
        if (map.get("photoMetadata") instanceof List) {
            List<Map<String, String>> metadata = new ArrayList<>();
            for (Object value : (List<?>) map.get("photoMetadata")) {
                if (!(value instanceof Map)) continue;
                Map<String, String> item = new HashMap<>();
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null) {
                        item.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
                    }
                }
                metadata.add(item);
            }
            post.setPhotoMetadata(metadata);
        }
        if (map.containsKey("caption") && map.get("caption") != null) post.setCaption((String) map.get("caption"));
        if (map.containsKey("colorTone") && map.get("colorTone") != null) post.setColorTone((String) map.get("colorTone"));
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
