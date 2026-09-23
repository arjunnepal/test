package com.c3s.ai.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProfileMathTest {

    @Test
    void percentRaisedRoundsAndClamps() {
        assertEquals(25, ProfileMath.percentRaised(1000, 250));
        assertEquals(33, ProfileMath.percentRaised(3000, 1000));
        assertEquals(67, ProfileMath.percentRaised(3000, 2000));
        assertEquals(100, ProfileMath.percentRaised(1000, 1500));
    }

    @Test
    void percentRaisedIsZeroWithoutAUsableGoal() {
        assertEquals(0, ProfileMath.percentRaised(null, 10));
        assertEquals(0, ProfileMath.percentRaised(0, 10));
        assertEquals(0, ProfileMath.percentRaised(1000, null));
    }

    @Test
    void displayEmojiPrefersTheProfilesOwn() {
        assertEquals("E", ProfileMath.displayEmoji("E", "H"));
        assertEquals("H", ProfileMath.displayEmoji("  ", "H"));
        assertEquals("H", ProfileMath.displayEmoji(null, "H"));
        assertNull(ProfileMath.displayEmoji(null, null));
    }
}
