package com.example.myapplication.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/** Extracts distinct Unicode hashtags without platform-specific regex flags. */
public final class HashtagParser {
    private HashtagParser() { }

    public static List<String> extract(String... texts) {
        LinkedHashSet<String> found = new LinkedHashSet<>();
        if (texts == null) return new ArrayList<>();
        for (String text : texts) {
            if (text == null) continue;
            for (int offset = 0; offset < text.length();) {
                int character = text.codePointAt(offset);
                if (character != '#') {
                    offset += Character.charCount(character);
                    continue;
                }
                int start = offset;
                offset += Character.charCount(character);
                int end = offset;
                while (end < text.length()) {
                    int next = text.codePointAt(end);
                    if (!Character.isLetterOrDigit(next) && next != '_') break;
                    end += Character.charCount(next);
                }
                if (end > offset) found.add(text.substring(start, end).toLowerCase(Locale.ROOT));
                offset = end;
            }
        }
        return new ArrayList<>(found);
    }
}
