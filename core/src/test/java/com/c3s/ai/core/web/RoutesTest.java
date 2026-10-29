package com.c3s.ai.core.web;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoutesTest {

    @Test
    void anAdminPageIsAPlaceToReturnTo() {
        assertEquals("/admin/categories", Routes.adminReturnPath("/admin/categories"));
        assertEquals("/admin/profiles/archive", Routes.adminReturnPath("/admin/profiles/archive"));
    }

    // "next" comes from the query string, so it must never become an open redirect
    @Test
    void anythingElseIsRefused() {
        for (String path : new String[]{null, "", "/", "/admin", "/admin/", "/admin/login", "/admin/logout",
                "//evil.example", "https://evil.example/admin/categories", "/admin//evil.example",
                "/admin/../login", "/admin/categories?x=1", "/admin/Categories", "/browse", "/report"}) {
            assertNull(Routes.adminReturnPath(path), String.valueOf(path));
        }
    }

    @Test
    void theReportIsNotAnAdminPath() {
        assertTrue(Routes.isReportPath("/report"));
        assertTrue(Routes.isReportPath("/report/login"));
        assertFalse(Routes.isReportPath("/reports"));
        assertFalse(Routes.isAdminPath("/report"));
    }
}
