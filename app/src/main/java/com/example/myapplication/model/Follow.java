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
        super(followerId + "_" + followingId);
        this.followerId = followerId;
        this.followingId = followingId;
    }

    public String getFollowerId() { return followerId; }
    public void setFollowerId(String followerId) { this.followerId = followerId; }

    public String getFollowingId() { return followingId; }
    public void setFollowingId(String followingId) { this.followingId = followingId; }

    @Override
    public boolean validate() {
        return followerId != null && !followerId.trim().isEmpty()
                && followingId != null && !followingId.trim().isEmpty();
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = baseMap();
        map.put("followerId", followerId);
        map.put("followingId", followingId);
        return map;
    }

    public static Follow fromMap(Map<String, Object> map) {
        if (map == null) return null;
        Follow follow = new Follow();
        if (map.containsKey("id") && map.get("id") != null) follow.setId((String) map.get("id"));
        if (map.containsKey("followerId") && map.get("followerId") != null) follow.setFollowerId((String) map.get("followerId"));
        if (map.containsKey("followingId") && map.get("followingId") != null) follow.setFollowingId((String) map.get("followingId"));
        return follow;
    }
}
