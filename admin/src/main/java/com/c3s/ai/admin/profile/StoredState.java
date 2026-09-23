package com.c3s.ai.admin.profile;

/** The two things a save needs to know about the stored profile: its status and its photo. */
public record StoredState(ProfileStatus status, String photoFilename) {
}
