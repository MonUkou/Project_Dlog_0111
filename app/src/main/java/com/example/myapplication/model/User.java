package com.example.myapplication.model;

import com.example.myapplication.BaseEntity;

import java.util.Map;

public class User extends BaseEntity {
    private String username;
    private String email;
    private String displayName;
    private String bio;
    private String avatarUrl;
    private int followerCount;
    private int followingCount;
    private int postsCount;

    public User() {
        super();
        this.followerCount = 0;
        this.followingCount = 0;
        this.postsCount = 0;
    }

    public User(String username, String email) {
        super();
        this.username = username;
        this.email = email;
        this.displayName = username != null ? username : "";
        this.bio = "";
        this.avatarUrl = "";
        this.followerCount = 0;
        this.followingCount = 0;
        this.postsCount = 0;
    }

    public User(String id, String username, String email) {
        super(id);
        this.username = username;
        this.email = email;
        this.displayName = username != null ? username : "";
        this.bio = "";
        this.avatarUrl = "";
        this.followerCount = 0;
        this.followingCount = 0;
        this.postsCount = 0;
    }

    public void updateProfile(String displayName, String bio, String avatarUrl) {
        if (displayName != null) {
            this.displayName = displayName;
        }
        if (bio != null) {
            this.bio = bio;
        }
        if (avatarUrl != null) {
            this.avatarUrl = avatarUrl;
        }
        touch();
    }

    public void adjustFollowersCount(int delta) {
        this.followerCount = Math.max(0, this.followerCount + delta);
        touch();
    }

    public void adjustFollowingCount(int delta) {
        this.followingCount = Math.max(0, this.followingCount + delta);
        touch();
    }

    public void adjustPostCount(int delta) {
        this.postsCount = Math.max(0, this.postsCount + delta);
        touch();
    }

    public void adjustPostsCount(int delta) {
        adjustPostCount(delta);
    }

    @Override
    public boolean validate() {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        if (followerCount < 0 || followingCount < 0 || postsCount < 0) {
            return false;
        }
        return true;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = baseMap();
        map.put("username", username);
        map.put("email", email);
        map.put("displayName", displayName != null ? displayName : "");
        map.put("bio", bio != null ? bio : "");
        map.put("avatarUrl", avatarUrl != null ? avatarUrl : "");
        map.put("followerCount", followerCount);
        map.put("followingCount", followingCount);
        map.put("postsCount", postsCount);
        map.put("postCount", postsCount);
        return map;
    }

    public static User fromMap(Map<String, Object> map, String id) {
        User user = new User();
        if (id != null) {
            user.setId(id);
        }
        if (map == null) {
            return user;
        }

        if (map.containsKey("id") && map.get("id") != null) {
            user.setId(String.valueOf(map.get("id")));
        }
        if (map.containsKey("username") && map.get("username") != null) {
            user.username = String.valueOf(map.get("username"));
        }
        if (map.containsKey("email") && map.get("email") != null) {
            user.email = String.valueOf(map.get("email"));
        }
        if (map.containsKey("displayName") && map.get("displayName") != null) {
            user.displayName = String.valueOf(map.get("displayName"));
        }
        if (map.containsKey("bio") && map.get("bio") != null) {
            user.bio = String.valueOf(map.get("bio"));
        }
        if (map.containsKey("avatarUrl") && map.get("avatarUrl") != null) {
            user.avatarUrl = String.valueOf(map.get("avatarUrl"));
        }

        user.followerCount = getIntValue(map, "followerCount", 0);
        user.followingCount = getIntValue(map, "followingCount", 0);

        int pCount = getIntValue(map, "postsCount", -1);
        if (pCount == -1) {
            pCount = getIntValue(map, "postCount", 0);
        }
        user.postsCount = Math.max(0, pCount);

        return user;
    }

    public static User fromMap(Map<String, Object> map) {
        return fromMap(map, null);
    }

    private static int getIntValue(Map<String, Object> map, String key, int defaultValue) {
        if (map.containsKey(key) && map.get(key) != null) {
            Object val = map.get(key);
            if (val instanceof Number) {
                return ((Number) val).intValue();
            }
            try {
                return Integer.parseInt(String.valueOf(val));
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public int getFollowerCount() {
        return followerCount;
    }

    public void setFollowerCount(int followerCount) {
        this.followerCount = Math.max(0, followerCount);
    }

    public int getFollowingCount() {
        return followingCount;
    }

    public void setFollowingCount(int followingCount) {
        this.followingCount = Math.max(0, followingCount);
    }

    public int getPostsCount() {
        return postsCount;
    }

    public void setPostsCount(int postsCount) {
        this.postsCount = Math.max(0, postsCount);
    }

    public int getPostCount() {
        return postsCount;
    }

    public void setPostCount(int postCount) {
        this.postsCount = Math.max(0, postCount);
    }
}
