package com.example.myapplication.model;

import com.example.myapplication.BaseEntity;
import java.util.Map;

public class User extends BaseEntity {
    private String username;
    private String fullName;
    private String email;
    private String avatarUrl;
    private String coverUrl;
    private String bio;
    private String role;
    private long postsCount;
    private long followersCount;
    private long followingCount;

    public User() {
        super();
    }

    public User(String id, String username, String fullName, String email, String avatarUrl, String bio) {
        super(id);
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.bio = bio;
        this.role = "user";
        this.postsCount = 0;
        this.followersCount = 0;
        this.followingCount = 0;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getRole() { return role; }

    public void setRole(String role) { this.role = role; }

    public long getPostsCount() {
        return postsCount;
    }

    public void setPostsCount(long postsCount) {
        this.postsCount = postsCount;
    }

    public long getFollowersCount() {
        return followersCount;
    }

    public void setFollowersCount(long followersCount) {
        this.followersCount = followersCount;
    }

    public long getFollowingCount() {
        return followingCount;
    }

    public void setFollowingCount(long followingCount) {
        this.followingCount = followingCount;
    }

    @Override
    public boolean validate() {
        return username != null && !username.trim().isEmpty()
                && email != null && !email.trim().isEmpty();
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = baseMap();
        map.put("username", username);
        map.put("fullName", fullName);
        map.put("email", email);
        map.put("avatarUrl", avatarUrl);
        map.put("coverUrl", coverUrl);
        map.put("bio", bio);
        map.put("role", role == null ? "user" : role);
        map.put("postsCount", postsCount);
        map.put("followersCount", followersCount);
        map.put("followingCount", followingCount);
        return map;
    }

    public static User fromMap(Map<String, Object> map) {
        if (map == null) return null;
        User user = new User();
        if (map.containsKey("id") && map.get("id") != null) user.setId((String) map.get("id"));
        if (map.containsKey("username") && map.get("username") != null) user.setUsername((String) map.get("username"));
        if (map.containsKey("fullName") && map.get("fullName") != null) user.setFullName((String) map.get("fullName"));
        if (map.containsKey("email") && map.get("email") != null) user.setEmail((String) map.get("email"));
        if (map.containsKey("avatarUrl") && map.get("avatarUrl") != null) user.setAvatarUrl((String) map.get("avatarUrl"));
        if (map.get("coverUrl") instanceof String) user.setCoverUrl((String) map.get("coverUrl"));
        if (map.containsKey("bio") && map.get("bio") != null) user.setBio((String) map.get("bio"));
        if (map.containsKey("role") && map.get("role") != null) user.setRole((String) map.get("role"));
        if (map.containsKey("postsCount") && map.get("postsCount") instanceof Number) {
            user.setPostsCount(((Number) map.get("postsCount")).longValue());
        }
        if (map.containsKey("followersCount") && map.get("followersCount") instanceof Number) {
            user.setFollowersCount(((Number) map.get("followersCount")).longValue());
        }
        if (map.containsKey("followingCount") && map.get("followingCount") instanceof Number) {
            user.setFollowingCount(((Number) map.get("followingCount")).longValue());
        }
        return user;
    }
}
