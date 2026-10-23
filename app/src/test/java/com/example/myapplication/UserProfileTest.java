package com.example.myapplication;
import com.example.myapplication.model.User;
import org.junit.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class UserProfileTest {
    @Test public void profileImagesSurviveFirestoreRoundTrip() {
        User user = new User("u1", "dlog", "DLog", "test@example.com", "https://storage/avatar.png", "bio");
        user.setCoverUrl("https://storage/cover.jpg");
        User restored = User.fromMap(user.toMap());
        assertEquals(user.getAvatarUrl(), restored.getAvatarUrl());
        assertEquals(user.getCoverUrl(), restored.getCoverUrl());
        assertEquals("u1", restored.getId());
    }
    @Test public void olderProfilesWithoutCoverStillLoad() {
        Map<String, Object> old = new HashMap<>();
        old.put("username", "existing"); old.put("avatarUrl", "");
        User restored = User.fromMap(old);
        assertEquals("existing", restored.getUsername());
        assertEquals("", restored.getAvatarUrl());
        assertNull(restored.getCoverUrl());
    }
}
