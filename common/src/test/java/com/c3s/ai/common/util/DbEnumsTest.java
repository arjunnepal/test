package com.c3s.ai.common.util;

import com.c3s.ai.common.exception.InvalidDataException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DbEnumsTest {

    private enum Colour { LIGHT_BLUE, RED }

    @Test
    void roundTripsLowerCaseValues() {
        assertEquals("light_blue", DbEnums.toDb(Colour.LIGHT_BLUE));
        assertEquals(Colour.LIGHT_BLUE, DbEnums.fromDb(Colour.class, "light_blue", Colour.RED));
        assertEquals(Colour.RED, DbEnums.fromDb(Colour.class, " RED ", Colour.LIGHT_BLUE));
        assertEquals(Colour.RED, DbEnums.fromDb(Colour.class, null, Colour.RED));
    }

    @Test
    void aCorruptDatabaseValueThrows() {
        assertThrows(InvalidDataException.class, () -> DbEnums.fromDb(Colour.class, "green", Colour.RED));
    }

    @Test
    void badRequestInputIsNullNotAnException() {
        assertNull(DbEnums.fromRequest(Colour.class, "green"));
        assertNull(DbEnums.fromRequest(Colour.class, " "));
        assertEquals(Colour.RED, DbEnums.fromRequest(Colour.class, "red"));
    }
}
