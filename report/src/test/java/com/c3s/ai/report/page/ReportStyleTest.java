package com.c3s.ai.report.page;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReportStyleTest {

    @Test
    void mapsEachKnownColorClass() {
        assertEquals("te", ReportStyle.forColorClass("cat-blue").getTagClass());
        assertEquals("th", ReportStyle.forColorClass("cat-red").getTagClass());
        assertEquals("tho", ReportStyle.forColorClass("cat-gold").getTagClass());
        assertEquals("tf", ReportStyle.forColorClass("cat-green").getTagClass());
        assertEquals("tv", ReportStyle.forColorClass("cat-purple").getTagClass());
        assertEquals("#92620a", ReportStyle.forColorClass("cat-gold").getAccentColor());
    }

    @Test
    void neutralAndUnknownAndNullHaveNoTagClass() {
        ReportStyle neutral = ReportStyle.forColorClass("cat-neutral");
        assertEquals(neutral, ReportStyle.forColorClass("cat-chartreuse"));
        assertEquals(neutral, ReportStyle.forColorClass(null));
        assertEquals("", neutral.getTagClass());
        assertEquals("var(--muted)", neutral.getAccentColor());
    }
}
