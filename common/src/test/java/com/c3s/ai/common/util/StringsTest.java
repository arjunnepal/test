package com.c3s.ai.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StringsTest {

    @Test
    void trimToNull() {
        assertNull(Strings.trimToNull(null));
        assertNull(Strings.trimToNull("   "));
        assertEquals("a b", Strings.trimToNull("  a b "));
    }

    @Test
    void longerThanCountsCodePoints() {
        assertFalse(Strings.longerThan(null, 0));
        // two supplementary-plane characters: 4 UTF-16 units, but only 2 code points
        String twoEmoji = new String(Character.toChars(0x1F393)) + new String(Character.toChars(0x1F3E5));
        assertFalse(Strings.longerThan(twoEmoji, 2));
        assertTrue(Strings.longerThan("abc", 2));
    }
}
