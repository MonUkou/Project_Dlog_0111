package com.example.myapplication.model;

import com.example.myapplication.BaseEntity;

import java.util.Map;

public class Follow extends BaseEntity {
    private String followerId;
    private String followingId;

    public Follow() {
        super();
    }

    public Follow(String followerId, String followingId) {
        super();
        this.followerId = followerId;
        this.followingId = followingId;
    }

    public Follow(String id, String followerId, String followingId) {
        super(id);
        this.followerId = followerId;
        this.followingId = followingId;
    }

    public String getFollowerId() {
        return followerId;
    }

    public void setFollowerId(String followerId) {
        this.followerId = followerId;
    }

    public String getFollowingId() {
        return followingId;
    }

    public void setFollowingId(String followingId) {
        this.followingId = followingId;
    }

    @Override
    public boolean validate() {
        if (followerId == null || followerId.trim().isEmpty()) {
            return false;
        }
        if (followingId == null || followingId.trim().isEmpty()) {
            return false;
        }
        // Prevent self-following
        if (followerId.equals(followingId)) {
            return false;
        }
        return true;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = baseMap();
        map.put("followerId", followerId);
        map.put("followingId", followingId);
        return map;
    }

    public static Follow fromMap(Map<String, Object> map, String id) {
        Follow follow = new Follow();
        if (id != null) {
            follow.setId(id);
        }
        if (map == null) {
            return follow;
        }
        if (map.containsKey("id") && map.get("id") != null) {
            follow.setId(String.valueOf(map.get("id")));
        }
        if (map.containsKey("followerId") && map.get("followerId") != null) {
            follow.followerId = String.valueOf(map.get("followerId"));
        }
        if (map.containsKey("followingId") && map.get("followingId") != null) {
            follow.followingId = String.valueOf(map.get("followingId"));
        }
        return follow;
    }

    public static Follow fromMap(Map<String, Object> map) {
        return fromMap(map, null);
    }
}
