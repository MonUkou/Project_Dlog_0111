package com.example.myapplication;
import com.example.myapplication.model.Post;
import com.example.myapplication.util.FeedPageSelector;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;

public class FeedPageSelectorTest {
    private Post post(String author) { Post p = new Post(); p.setAuthorId(author); p.setCreatedAt(1L); return p; }
    @Test public void skipsSelfAndRetainsCursorForUnreadPosts() {
        Post a = post("friend-a"), b = post("friend-b"), c = post("friend-c");
        FeedPageSelector.Selection page = FeedPageSelector.select(
                Arrays.asList(post("me"), a, post("me"), b, c), "me", 2);
        assertEquals(Arrays.asList(a,b), page.posts);
        assertEquals(3, page.lastConsumedIndex);
        assertTrue(page.hasUnreadItems);
        FeedPageSelector.Selection next = FeedPageSelector.select(Collections.singletonList(c), "me", 2);
        assertEquals(Collections.singletonList(c), next.posts);
        assertFalse(next.hasUnreadItems);
    }
    @Test public void selfOnlyPageStillAdvancesCursor() {
        FeedPageSelector.Selection page = FeedPageSelector.select(Arrays.asList(post("me"),post("me")), "me", 10);
        assertTrue(page.posts.isEmpty()); assertEquals(1,page.lastConsumedIndex);
        assertFalse(page.hasUnreadItems);
    }
    @Test public void anonymousViewerDoesNotHideOtherUsers() {
        Post p = post("someone");
        assertEquals(Collections.singletonList(p), FeedPageSelector.select(Collections.singletonList(p), null, 10).posts);
    }
    @Test public void missingDocumentsAndAuthorsDoNotBreakPagination() {
        FeedPageSelector.Selection page = FeedPageSelector.select(Arrays.asList(null,post(null),post("me")), "me", 10);
        assertTrue(page.posts.isEmpty()); assertEquals(2,page.lastConsumedIndex);
        assertEquals(-1, FeedPageSelector.select(Collections.emptyList(), "me", 10).lastConsumedIndex);
    }
    @Test public void ownNewPostAppearsForThirtyMinutesOnly() {
        long now = 3_000_000L;
        Post recent = post("me"); recent.setCreatedAt(now - 1_000L);
        Post boundary = post("me"); boundary.setCreatedAt(now - 1_800_000L);
        Post friend = post("friend"); friend.setCreatedAt(now - 2_000_000L);
        assertEquals(Arrays.asList(recent, friend), FeedPageSelector.select(
                Arrays.asList(recent, boundary, friend), "me", 10, now).posts);
    }
    @Test public void hiddenPostDoesNotConsumeVisibleSlot() {
        Post hidden = post("friend-a"); hidden.setHidden(true);
        Post visible = post("friend-b");
        FeedPageSelector.Selection page = FeedPageSelector.select(Arrays.asList(hidden, visible), "me", 1);
        assertEquals(Collections.singletonList(visible), page.posts);
        assertEquals(1, page.lastConsumedIndex);
    }
}
