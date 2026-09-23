package com.c3s.ai.admin.profile;

public record SidebarStats(long publishedProfiles, long totalRaised) {

    public static final SidebarStats EMPTY = new SidebarStats(0, 0);
}
