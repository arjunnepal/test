package com.c3s.ai.admin.shell;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PendingUploadsTest {

    /** A request whose session is just an attribute map; a null map = no session yet. */
    private static HttpServletRequest request(Map<String, Object>[] attributes) {
        HttpSession session = (HttpSession) Proxy.newProxyInstance(PendingUploadsTest.class.getClassLoader(),
                new Class<?>[]{HttpSession.class}, (proxy, m, args) -> switch (m.getName()) {
                    case "getAttribute" -> attributes[0].get((String) args[0]);
                    case "setAttribute" -> attributes[0].put((String) args[0], args[1]);
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> null;
                });
        return (HttpServletRequest) Proxy.newProxyInstance(PendingUploadsTest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class}, (proxy, m, args) -> {
                    if (m.getName().equals("getSession")) {
                        boolean create = args == null || (Boolean) args[0];
                        if (attributes[0] == null && create) {
                            attributes[0] = new HashMap<>();
                        }
                        return attributes[0] == null ? null : session;
                    }
                    return null;
                });
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object>[] newSession() {
        return new Map[]{null};
    }

    @Test
    void onlyThisSessionsUploadsAreHonoured() {
        HttpServletRequest mine = request(newSession());
        HttpServletRequest someoneElse = request(newSession());

        PendingUploads.add(mine, "a.png");

        assertTrue(PendingUploads.contains(mine, "a.png"));
        assertFalse(PendingUploads.contains(someoneElse, "a.png"), "another staff member's pending photo");
        assertFalse(PendingUploads.contains(mine, "b.png"), "an existing profile's photo name");
    }

    @Test
    void removedAndSessionlessAreNotHonoured() {
        HttpServletRequest mine = request(newSession());
        PendingUploads.add(mine, "a.png");
        PendingUploads.remove(mine, "a.png");
        assertFalse(PendingUploads.contains(mine, "a.png"));

        assertFalse(PendingUploads.contains(request(newSession()), "a.png"));
        assertFalse(PendingUploads.contains(mine, null));
    }
}
