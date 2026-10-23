package com.example.myapplication;

import com.example.myapplication.model.Post;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class PostSerializationTest {
    @Test public void keepsPhotoOrderAndOriginalPostingTime() {
        Post post = new Post();
        post.setAuthorId("author");
        post.setImageUrl("https://example.com/first.jpg");
        post.setImageUrls(Arrays.asList("https://example.com/first.jpg", "https://example.com/second.jpg"));
        post.setCreatedAt(123456L);
        Post restored = Post.fromMap(post.toMap());
        assertEquals(post.getImageUrls(), restored.getImageUrls());
        assertEquals(123456L, restored.getCreatedAt());
    }
}
