package com.c3s.ai.core.user;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoleTest {

    @Test
    void staffIsAdministratorAndEmployeeOnly() {
        assertTrue(Role.ADMINISTRATOR.isStaff());
        assertTrue(Role.EMPLOYEE.isStaff());
        assertFalse(Role.USER.isStaff());
    }

    @Test
    void onlyAdministratorIsAdministrator() {
        assertTrue(Role.ADMINISTRATOR.isAdministrator());
        assertFalse(Role.EMPLOYEE.isAdministrator());
        assertFalse(Role.USER.isAdministrator());
    }

    @Test
    void parsesTheStoredValues() {
        assertEquals(Role.ADMINISTRATOR, Role.fromDb("ADMINISTRATOR"));
        assertEquals(Role.EMPLOYEE, Role.fromDb("employee"));
        assertEquals(Role.USER, Role.fromDb("  User  "));
    }

    // an unreadable value must never be read as more access than it says
    @Test
    void unknownAndMissingValuesFallBackToTheLeastPrivilege() {
        assertEquals(Role.USER, Role.fromDb(null));
        assertEquals(Role.USER, Role.fromDb(""));
        assertEquals(Role.USER, Role.fromDb("   "));
        assertEquals(Role.USER, Role.fromDb("SUPERUSER"));
        assertEquals(Role.USER, Role.fromDb("admin"));
    }

    @Test
    void labelsForThePage() {
        assertEquals("Administrator", Role.ADMINISTRATOR.label());
        assertEquals("Employee", Role.EMPLOYEE.label());
    }
}
