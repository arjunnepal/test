package com.c3s.ai.browse.page;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BrowseStyleTest {

    @Test
    void mapsEachKnownColorClass() {
        assertEquals("te", BrowseStyle.forColorClass("cat-blue").getTagClass());
        assertEquals("th", BrowseStyle.forColorClass("cat-red").getTagClass());
        assertEquals("tho", BrowseStyle.forColorClass("cat-gold").getTagClass());
        assertEquals("tf", BrowseStyle.forColorClass("cat-green").getTagClass());
        assertEquals("tv", BrowseStyle.forColorClass("cat-purple").getTagClass());
    }

    @Test
    void usesThePrototypesClassesAndColours() {
        assertEquals("cat-he", BrowseStyle.forColorClass("cat-red").getChipClass());
        assertEquals("cat-ve", BrowseStyle.forColorClass("cat-purple").getChipClass());
        assertEquals("#92620a", BrowseStyle.forColorClass("cat-gold").getAccentColor());
        assertEquals("linear-gradient(135deg,#F0E8FF,#d4b8f0)", BrowseStyle.forColorClass("cat-purple").getCardBackground());
    }

    @Test
    void neutralAndUnknownAndNullFallBackToBlue() {
        BrowseStyle blue = BrowseStyle.forColorClass("cat-blue");
        assertEquals(blue, BrowseStyle.forColorClass("cat-neutral"));
        assertEquals(blue, BrowseStyle.forColorClass("cat-chartreuse"));
        assertEquals(blue, BrowseStyle.forColorClass(null));
    }
}
