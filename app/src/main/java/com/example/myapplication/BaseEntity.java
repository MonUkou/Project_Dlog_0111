package com.example.myapplication;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseEntity {

    private String id;
    private long createdAt;
    private long updatedAt;

    public BaseEntity() {
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public BaseEntity(String id) {
        this();
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void touch() {
        this.updatedAt = System.currentTimeMillis();
    }

    public Map<String, Object> baseMap() {
        Map<String, Object> map = new HashMap<>();

        map.put("id", id);
        map.put("createdAt", createdAt);
        map.put("updatedAt", updatedAt);

        return map;
    }

    public abstract boolean validate();

    public abstract Map<String, Object> toMap();
}
