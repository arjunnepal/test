package com.c3s.ai.admin.dao;

import com.c3s.ai.admin.exception.EntityNotFoundException;
import com.c3s.ai.admin.model.Profile;

import java.util.List;
import java.util.Optional;

public interface ProfileDao {

    Profile findById(long id) throws EntityNotFoundException;

    /** activeOnly=true drops archived profiles. */
    List<Profile> findAll(boolean activeOnly);

    /** No categories loaded - just enough to read the name. */
    Optional<Profile> findFirst(boolean activeOnly);

    long insert(Profile profile);

    void update(Profile profile) throws EntityNotFoundException;

    /** Soft-delete - still returned by findAll(false) as Archived. */
    void archive(long id) throws EntityNotFoundException;

    /** Published-profile count + combined raised total, for the sidebar. */
    SidebarStats getSidebarStats();

    record SidebarStats(long publishedProfiles, long totalRaised) {
    }
}
