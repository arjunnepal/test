package com.c3s.ai.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogTest {

    @Test
    void replacesEveryKindOfLineBreakAndControlCharacter() {
        String input = "a" + (char) 10 + "b" + (char) 13 + "c" + (char) 9 + "d"
                + (char) 0x85 + "e" + (char) 0x2028 + "f" + (char) 0x2029 + "g";
        assertEquals("a_b_c_d_e_f_g", Log.clean(input));
        assertEquals("x_y", Log.clean("x" + (char) 0x7f + "y"));
        assertEquals("x_y", Log.clean("x" + (char) 0x9b + "y"));
    }

    @Test
    void keepsOrdinaryAccentedText() {
        assertEquals("Jos" + (char) 0xe9, Log.clean("Jos" + (char) 0xe9));
    }

    @Test
    void truncatesAndIsNullSafe() {
        assertEquals("null", Log.clean(null));
        String cleaned = Log.clean("x".repeat(300));
        assertTrue(cleaned.endsWith("..."));
        assertEquals(259, cleaned.length());
    }
}
