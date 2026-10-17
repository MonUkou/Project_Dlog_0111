package com.example.myapplication.model;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class UserTest {

    @Test
    public void testConstructorsAndGetters() {
        User defaultUser = new User();
        assertNull(defaultUser.getUsername());
        assertNull(defaultUser.getEmail());
        assertEquals(0, defaultUser.getFollowerCount());
        assertEquals(0, defaultUser.getFollowingCount());
        assertEquals(0, defaultUser.getPostsCount());

        User paramUser = new User("testuser", "test@example.com");
        assertEquals("testuser", paramUser.getUsername());
        assertEquals("test@example.com", paramUser.getEmail());
        assertEquals("testuser", paramUser.getDisplayName());
        assertEquals("", paramUser.getBio());
        assertEquals("", paramUser.getAvatarUrl());
        assertEquals(0, paramUser.getFollowerCount());

        User idUser = new User("user123", "iduser", "id@example.com");
        assertEquals("user123", idUser.getId());
        assertEquals("iduser", idUser.getUsername());
        assertEquals("id@example.com", idUser.getEmail());
    }

    @Test
    public void testUpdateProfile() {
        User user = new User("alice", "alice@example.com");
        user.updateProfile("Alice Wonderland", "Hello world", "https://example.com/avatar.png");

        assertEquals("Alice Wonderland", user.getDisplayName());
        assertEquals("Hello world", user.getBio());
        assertEquals("https://example.com/avatar.png", user.getAvatarUrl());
    }

    @Test
    public void testAdjustCounts() {
        User user = new User("bob", "bob@example.com");
        
        user.adjustFollowersCount(5);
        assertEquals(5, user.getFollowerCount());
        user.adjustFollowersCount(-2);
        assertEquals(3, user.getFollowerCount());
        user.adjustFollowersCount(-10); // should not go below 0
        assertEquals(0, user.getFollowerCount());

        user.adjustFollowingCount(10);
        assertEquals(10, user.getFollowingCount());
        user.adjustFollowingCount(-15);
        assertEquals(0, user.getFollowingCount());

        user.adjustPostCount(3);
        assertEquals(3, user.getPostsCount());
        user.adjustPostCount(-5);
        assertEquals(0, user.getPostsCount());
    }

    @Test
    public void testValidate() {
        User validUser = new User("user1", "user1@example.com");
        assertTrue(validUser.validate());

        User invalidUsername = new User("", "user1@example.com");
        assertFalse(invalidUsername.validate());

        User nullUsername = new User(null, "user1@example.com");
        assertFalse(nullUsername.validate());

        User invalidEmail = new User("user1", "");
        assertFalse(invalidEmail.validate());

        User nullEmail = new User("user1", null);
        assertFalse(nullEmail.validate());
    }

    @Test
    public void testToMapAndFromMap() {
        User user = new User("u1", "charlie", "charlie@example.com");
        user.updateProfile("Charlie C", "Developer", "url");
        user.adjustFollowersCount(10);
        user.adjustFollowingCount(5);
        user.adjustPostCount(2);

        Map<String, Object> map = user.toMap();
        assertEquals("u1", map.get("id"));
        assertEquals("charlie", map.get("username"));
        assertEquals("charlie@example.com", map.get("email"));
        assertEquals("Charlie C", map.get("displayName"));
        assertEquals("Developer", map.get("bio"));
        assertEquals("url", map.get("avatarUrl"));
        assertEquals(10, map.get("followerCount"));
        assertEquals(5, map.get("followingCount"));
        assertEquals(2, map.get("postsCount"));

        // Restore from map
        User restored = User.fromMap(map);
        assertEquals("u1", restored.getId());
        assertEquals("charlie", restored.getUsername());
        assertEquals("charlie@example.com", restored.getEmail());
        assertEquals("Charlie C", restored.getDisplayName());
        assertEquals("Developer", restored.getBio());
        assertEquals("url", restored.getAvatarUrl());
        assertEquals(10, restored.getFollowerCount());
        assertEquals(5, restored.getFollowingCount());
        assertEquals(2, restored.getPostsCount());
    }

    @Test
    public void testFromMapMissingOptionalFields() {
        // Document missing optional fields and some counts
        Map<String, Object> sparseMap = new HashMap<>();
        sparseMap.put("username", "david");
        sparseMap.put("email", "david@example.com");
        // displayName, bio, avatarUrl, followerCount, etc. are missing

        User user = User.fromMap(sparseMap, "user_david");
        assertEquals("user_david", user.getId());
        assertEquals("david", user.getUsername());
        assertEquals("david@example.com", user.getEmail());
        assertEquals(0, user.getFollowerCount());
        assertEquals(0, user.getFollowingCount());
        assertEquals(0, user.getPostsCount());
        assertTrue(user.validate());
    }

    @Test
    public void testSecurityNoPassword() {
        // Verify that User model does not contain any password field or password methods via reflection or inspection
        try {
            User.class.getDeclaredField("password");
            fail("User model should not contain a password field!");
        } catch (NoSuchFieldException e) {
            // Expected
        }
    }
}
