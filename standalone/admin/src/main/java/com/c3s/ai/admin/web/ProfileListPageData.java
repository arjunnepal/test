package com.c3s.ai.admin.web;

import com.c3s.ai.admin.model.Profile;

import java.util.List;

// page object for profiles-list.jsp
public class ProfileListPageData extends PageData {

    private List<Profile> profiles;

    public List<Profile> getProfiles() {
        return profiles;
    }

    public void setProfiles(List<Profile> profiles) {
        this.profiles = profiles;
    }
}
