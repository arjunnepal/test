package com.c3s.ai.admin.profile;

import com.c3s.ai.common.util.Strings;

final class Location {

    private Location() {
    }

    /** "Jacksonville, FL", "Jacksonville", "FL" or "". */
    static String of(String city, String state) {
        boolean hasCity = !Strings.isBlank(city);
        boolean hasState = !Strings.isBlank(state);
        if (hasCity && hasState) {
            return city.trim() + ", " + state.trim();
        }
        return hasCity ? city.trim() : hasState ? state.trim() : "";
    }
}
