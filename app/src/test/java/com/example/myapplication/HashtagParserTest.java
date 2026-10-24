package com.example.myapplication;

import com.example.myapplication.util.HashtagParser;
import com.example.myapplication.util.PostDisplayText;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class HashtagParserTest {
    @Test public void extractsVietnameseTagsFromCaptionAndField() {
        assertEquals(Arrays.asList("#đàlạt", "#ảnh_đẹp", "#ueh2026"),
                HashtagParser.extract("Đi chơi #ĐàLạt #ảnh_đẹp", "#ĐÀLẠT #UEH2026"));
    }

    @Test public void ignoresEmptyMarker() {
        assertEquals(Arrays.asList("#ảnh"), HashtagParser.extract("# @abc #ảnh"));
    }

    @Test public void displayedCaptionDoesNotRepeatTypedHashtags() {
        assertEquals("Ở Đà Lạt #dulich\n#ueh", PostDisplayText.captionWithTags(
                "Ở Đà Lạt #dulich", Arrays.asList("#dulich", "#ueh")));
    }
}
