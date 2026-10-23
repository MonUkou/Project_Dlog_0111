package com.example.myapplication.service.impl;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.myapplication.model.Post;
import com.example.myapplication.model.User;
import com.example.myapplication.util.FeedPageSelector;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A small, per-account snapshot of pages the user has actually opened. */
public final class OfflinePages extends SQLiteOpenHelper {
    public OfflinePages(Context context) { super(context.getApplicationContext(), "dlog_pages.db", null, 1); }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE pages (page_key TEXT PRIMARY KEY, body TEXT NOT NULL, saved_at INTEGER NOT NULL)");
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { }

    private void put(String key, String body) {
        ContentValues values = new ContentValues();
        values.put("page_key", key);
        values.put("body", body);
        values.put("saved_at", System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict("pages", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }
    private String get(String key) {
        try (Cursor cursor = getReadableDatabase().query("pages", new String[]{"body"}, "page_key=?", new String[]{key}, null, null, null)) {
            return cursor.moveToFirst() ? cursor.getString(0) : null;
        }
    }
    public void saveFeed(String viewerId, List<Post> posts) {
        saveFeedPage(viewerId, posts, true);
    }

    /** Stores every page the user has reached, preserving order and replacing updated posts by id. */
    public synchronized void saveFeedPage(String viewerId, List<Post> posts, boolean reset) {
        if (viewerId == null || posts == null) return;
        LinkedHashMap<String, Post> merged = new LinkedHashMap<>();
        if (!reset) {
            for (Post cached : feed(viewerId)) {
                if (cached != null && cached.getId() != null) merged.put(cached.getId(), cached);
            }
        }
        for (Post post : posts) {
            if (post != null && post.getId() != null) merged.put(post.getId(), post);
        }
        JSONArray array = new JSONArray();
        for (Post post : merged.values()) array.put(new JSONObject(post.toMap()));
        put("feed:" + viewerId, array.toString());
        for (Post post : posts) {
            if (post != null && post.getId() != null) put("post:" + post.getId(), new JSONObject(post.toMap()).toString());
        }
    }
    public List<Post> feed(String viewerId) {
        List<Post> posts = new ArrayList<>();
        if (viewerId == null) return posts;
        try {
            String body = get("feed:" + viewerId);
            if (body == null) return posts;
            JSONArray array = new JSONArray(body);
            for (int i = 0; i < array.length(); i++) {
                Post post = postFromJson(array.getJSONObject(i));
                if (post != null && !FeedPageSelector.select(
                        java.util.Collections.singletonList(post), viewerId, 1).posts.isEmpty()) posts.add(post);
            }
        } catch (Exception ignored) { }
        return posts;
    }

    public Post post(String postId) {
        if (postId == null) return null;
        try {
            String body = get("post:" + postId);
            return body == null ? null : postFromJson(new JSONObject(body));
        } catch (Exception ignored) { return null; }
    }

    public void savePost(Post post) {
        if (post != null && post.getId() != null) {
            put("post:" + post.getId(), new JSONObject(post.toMap()).toString());
        }
    }

    public List<Post> postsWithTag(String viewerId, String tag) {
        List<Post> matches = new ArrayList<>();
        if (tag == null) return matches;
        String normalized = tag.startsWith("#") ? tag.toLowerCase() : "#" + tag.toLowerCase();
        for (Post post : feed(viewerId)) {
            for (String value : post.getHashtags()) {
                if (normalized.equals(value.toLowerCase())) { matches.add(post); break; }
            }
        }
        return matches;
    }

    public List<Post> postsWithTone(String viewerId, String tone) {
        List<Post> matches = new ArrayList<>();
        if (tone == null) return matches;
        for (Post post : feed(viewerId)) {
            if (tone.equals(post.getColorTone())) matches.add(post);
        }
        return matches;
    }

    private Post postFromJson(JSONObject item) throws Exception {
        Map<String, Object> map = new java.util.HashMap<>();
        java.util.Iterator<String> keys = item.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = item.get(key);
            if (value instanceof JSONArray && ("imageUrls".equals(key) || "hashtags".equals(key))) {
                JSONArray values = (JSONArray) value;
                List<String> strings = new ArrayList<>();
                for (int j = 0; j < values.length(); j++) strings.add(values.optString(j));
                value = strings;
            } else if (value instanceof JSONArray && "photoMetadata".equals(key)) {
                JSONArray values = (JSONArray) value;
                List<Map<String, String>> metadata = new ArrayList<>();
                for (int j = 0; j < values.length(); j++) {
                    JSONObject object = values.optJSONObject(j);
                    if (object == null) continue;
                    Map<String, String> fields = new java.util.HashMap<>();
                    java.util.Iterator<String> fieldKeys = object.keys();
                    while (fieldKeys.hasNext()) {
                        String field = fieldKeys.next();
                        fields.put(field, object.optString(field));
                    }
                    metadata.add(fields);
                }
                value = metadata;
            }
            map.put(key, value);
        }
        return Post.fromMap(map);
    }
    public void saveProfile(String userId, User user) {
        if (userId != null && user != null) put("profile:" + userId, new JSONObject(user.toMap()).toString());
    }
    public User profile(String userId) {
        if (userId == null) return null;
        try {
            String body = get("profile:" + userId);
            if (body == null) return null;
            JSONObject item = new JSONObject(body);
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            java.util.Iterator<String> keys = item.keys();
            while (keys.hasNext()) { String key = keys.next(); map.put(key, item.get(key)); }
            return User.fromMap(map);
        } catch (Exception ignored) { return null; }
    }
}
