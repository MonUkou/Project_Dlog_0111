package com.example.myapplication.util;

import com.example.myapplication.model.Post;
import java.util.ArrayList;
import java.util.List;

/** Selects a visible page while preserving the position in the unfiltered feed. */
public final class FeedPageSelector {
    /** Keep a newly published post in its author's feed for 30 minutes. */
    public static final long OWN_POST_VISIBILITY_MS = 30L * 60L * 1000L;
    private FeedPageSelector() {}
    public static final class Selection {
        public final List<Post> posts;
        public final int lastConsumedIndex;
        public final boolean hasUnreadItems;
        Selection(List<Post> posts, int lastConsumedIndex, boolean hasUnreadItems) {
            this.posts = posts; this.lastConsumedIndex = lastConsumedIndex;
            this.hasUnreadItems = hasUnreadItems;
        }
    }
    public static Selection select(List<Post> raw, String viewerId, int capacity) {
        return select(raw, viewerId, capacity, System.currentTimeMillis());
    }
    public static Selection select(List<Post> raw, String viewerId, int capacity, long now) {
        if (capacity <= 0) throw new IllegalArgumentException("Page capacity must be positive");
        List<Post> visible = new ArrayList<>();
        int consumed = -1;
        for (int i = 0; i < raw.size(); i++) {
            consumed = i;
            Post post = raw.get(i);
            if (post != null && !post.isHidden() && post.getAuthorId() != null) {
                boolean own = post.getAuthorId().equals(viewerId);
                long age = now - post.getCreatedAt();
                if (!own || (age >= 0 && age < OWN_POST_VISIBILITY_MS)) visible.add(post);
            }
            if (visible.size() == capacity) break;
        }
        return new Selection(visible, consumed, consumed + 1 < raw.size());
    }
}
