package com.example.myapplication;

import com.example.myapplication.model.Post;
import com.example.myapplication.model.SavedPost;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class PostTest {

    @Test
    public void testPostValidationSuccess() {
        Post post = new Post();
        post.setId("post1");
        post.setAuthorId("user1");
        post.setImageUrl("https://example.com/image.jpg");
        post.setCaption("Hello DLog");

        assertTrue(post.validate());
        assertEquals("user1", post.getAuthorId());
        assertEquals("https://example.com/image.jpg", post.getImageUrl());
        assertEquals("Hello DLog", post.getCaption());
    }

    @Test
    public void testPostValidationFailureMissingAuthor() {
        Post post = new Post();
        post.setId("post1");
        post.setImageUrl("https://example.com/image.jpg");

        assertFalse(post.validate());
    }

    @Test
    public void testPostValidationFailureMissingImage() {
        Post post = new Post();
        post.setId("post1");
        post.setAuthorId("user1");

        assertFalse(post.validate());
    }

    @Test
    public void testPostToMapAndFromMap() {
        Post post = new Post("post1", "minh.anh", "https://storage/image.jpg", "Hoàng hôn Vũng Tàu", 128, 3);
        post.setAuthorUsername("minh.anh");

        Map<String, Object> map = post.toMap();
        assertEquals("post1", map.get("id"));
        assertEquals("minh.anh", map.get("authorId"));
        assertEquals("https://storage/image.jpg", map.get("imageUrl"));
        assertEquals("Hoàng hôn Vũng Tàu", map.get("caption"));
        assertEquals(128L, map.get("likesCount"));
        assertEquals(3L, map.get("commentsCount"));

        Post restored = Post.fromMap(map);
        assertNotNull(restored);
        assertEquals("post1", restored.getId());
        assertEquals("minh.anh", restored.getAuthorId());
        assertEquals("https://storage/image.jpg", restored.getImageUrl());
        assertEquals("Hoàng hôn Vũng Tàu", restored.getCaption());
        assertEquals(128L, restored.getLikesCount());
        assertEquals(3L, restored.getCommentsCount());
    }

    @Test
    public void testSavedPostValidationAndDocId() {
        SavedPost savedPost = new SavedPost("user1", "post1");
        assertTrue(savedPost.validate());
        assertEquals("user1_post1", savedPost.getId());
        assertEquals("user1", savedPost.getUserId());
        assertEquals("post1", savedPost.getPostId());

        Map<String, Object> map = savedPost.toMap();
        SavedPost restored = SavedPost.fromMap(map);
        assertNotNull(restored);
        assertEquals("user1_post1", restored.getId());
        assertEquals("user1", restored.getUserId());
        assertEquals("post1", restored.getPostId());
    }

    @Test
    public void testSavedPostValidationFailure() {
        SavedPost savedPost = new SavedPost();
        assertFalse(savedPost.validate());

        savedPost.setUserId("user1");
        assertFalse(savedPost.validate());

        savedPost.setPostId("post1");
        assertTrue(savedPost.validate());
    }
}
