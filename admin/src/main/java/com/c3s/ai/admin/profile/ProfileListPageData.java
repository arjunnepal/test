package com.c3s.ai.admin.profile;

import com.c3s.ai.core.web.PageData;

import java.util.List;

/** Page object for profiles-list.jsp */
public class ProfileListPageData extends PageData {

    private final List<ProfileListItem> profiles;

    public ProfileListPageData(List<ProfileListItem> profiles) {
        this.profiles = List.copyOf(profiles);
    }

    public List<ProfileListItem> getProfiles() {
        return profiles;
    }
}
