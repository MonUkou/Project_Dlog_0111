package com.example.myapplication.service.impl;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.myapplication.model.Post;
import com.example.myapplication.model.User;
import com.example.myapplication.model.Comment;
import com.example.myapplication.util.FeedPageSelector;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A small, per-account snapshot of pages the user has actually opened. */
public final class OfflinePages extends SQLiteOpenHelper {
    public static final int MAX_OFFLINE_FEED_POSTS = 5;
    private static final int DATABASE_VERSION = 2;
    public OfflinePages(Context context) {
        super(context.getApplicationContext(), "dlog_pages.db", null, DATABASE_VERSION);
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE pages (page_key TEXT PRIMARY KEY, body TEXT NOT NULL, saved_at INTEGER NOT NULL)");
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Version 2 starts the strict per-login-session cache and removes legacy snapshots.
        db.execSQL("DROP TABLE IF EXISTS pages");
        onCreate(db);
    }

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

    private boolean contains(String key) {
        try (Cursor cursor = getReadableDatabase().query("pages", new String[]{"page_key"},
                "page_key=?", new String[]{key}, null, null, null)) {
            return cursor.moveToFirst();
        }
    }

    /** Starts a clean SQLite snapshot for one explicit sign-in session. */
    public synchronized void beginSession(String userId) {
        getWritableDatabase().delete("pages", null, null);
    }

    /** Removes private offline data when the current session ends. */
    public synchronized void clearSession() {
        getWritableDatabase().delete("pages", null, null);
    }
    public void saveFeed(String viewerId, List<Post> posts) {
        saveFeedPage(viewerId, posts, true);
    }

    /** Stores at most five feed posts for the current sign-in session. */
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
        List<Post> retained = new ArrayList<>();
        for (Post post : merged.values()) {
            if (retained.size() >= MAX_OFFLINE_FEED_POSTS) break;
            retained.add(post);
            array.put(new JSONObject(post.toMap()));
        }
        put("feed:" + viewerId, array.toString());
        for (Post post : retained) {
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

    /** Explicit cache for the photo grid belonging to a profile the user opened. */
    public void saveProfilePosts(String userId, List<Post> posts) {
        savePostList("profile_posts:" + userId, posts);
    }

    public List<Post> profilePosts(String userId) {
        return readPostList("profile_posts:" + userId);
    }

    public boolean hasProfilePosts(String userId) {
        return userId != null && contains("profile_posts:" + userId);
    }

    /** Explicit cache for the current account's Saved tab. */
    public void saveSavedPosts(String userId, List<Post> posts) {
        savePostList("saved_posts:" + userId, posts);
    }

    public List<Post> savedPosts(String userId) {
        return readPostList("saved_posts:" + userId);
    }

    public boolean hasSavedPosts(String userId) {
        return userId != null && contains("saved_posts:" + userId);
    }

    /** Comments are cached only after their panel has been opened online at least once. */
    public void saveComments(String postId, List<Comment> comments) {
        if (postId == null || comments == null) return;
        JSONArray array = new JSONArray();
        for (Comment comment : comments) {
            if (comment != null) array.put(new JSONObject(comment.toMap()));
        }
        put("comments:" + postId, array.toString());
    }

    public boolean hasComments(String postId) {
        return postId != null && contains("comments:" + postId);
    }

    public List<Comment> comments(String postId) {
        List<Comment> comments = new ArrayList<>();
        if (postId == null) return comments;
        try {
            String body = get("comments:" + postId);
            if (body == null) return comments;
            JSONArray array = new JSONArray(body);
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                Comment comment = new Comment();
                comment.setId(nullableString(item, "id"));
                comment.setPostId(nullableString(item, "postId"));
                comment.setAuthorId(nullableString(item, "authorId"));
                comment.setUsername(nullableString(item, "username"));
                comment.setAvatarUrl(nullableString(item, "avatarUrl"));
                comment.setContent(nullableString(item, "content"));
                comment.setHidden(item.optBoolean("hidden", false));
                comment.setCreatedAt(item.optLong("createdAt", 0L));
                comment.setUpdatedAt(item.optLong("updatedAt", 0L));
                comments.add(comment);
            }
        } catch (Exception ignored) { }
        return comments;
    }

    private void savePostList(String key, List<Post> posts) {
        if (key == null || posts == null) return;
        JSONArray array = new JSONArray();
        for (Post post : posts) {
            if (post != null) array.put(new JSONObject(post.toMap()));
        }
        put(key, array.toString());
    }

    private List<Post> readPostList(String key) {
        List<Post> posts = new ArrayList<>();
        if (key == null) return posts;
        try {
            String body = get(key);
            if (body == null) return posts;
            JSONArray array = new JSONArray(body);
            for (int i = 0; i < array.length(); i++) {
                Post post = postFromJson(array.getJSONObject(i));
                if (post != null) posts.add(post);
            }
        } catch (Exception ignored) { }
        return posts;
    }

    private String nullableString(JSONObject item, String key) {
        if (!item.has(key) || item.isNull(key)) return null;
        String value = item.optString(key, null);
        return value == null || "null".equals(value) ? null : value;
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
    public boolean hasProfile(String userId) {
        return userId != null && contains("profile:" + userId);
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
