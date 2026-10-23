package com.example.myapplication.util;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class PostDisplayText {
    private PostDisplayText() { }

    public static String captionWithTags(String caption, List<String> hashtags) {
        String body = caption == null ? "" : caption;
        if (hashtags == null || hashtags.isEmpty()) return body;
        Set<String> present = new HashSet<>(HashtagParser.extract(body));
        StringBuilder missing = new StringBuilder();
        for (String tag : hashtags) {
            if (tag == null || tag.isEmpty()) continue;
            String normalized = tag.toLowerCase(Locale.ROOT);
            if (present.add(normalized)) {
                if (missing.length() > 0) missing.append(' ');
                missing.append(tag);
            }
        }
        return missing.length() == 0 ? body : body + (body.isEmpty() ? "" : "\n") + missing;
    }
}
