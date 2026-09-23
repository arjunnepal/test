package com.c3s.ai.admin.profile;

import java.util.List;
import java.util.Optional;

/** Package-private: every profile read and write goes through ProfileService and its rules. */
interface ProfileDao {

    /** Every profile, any status, newest first, with its categories - list columns only. */
    List<ProfileListItem> findListItems();

    /** The full profile for its edit form. */
    Optional<Profile> findById(long id);

    Optional<StoredState> findStoredState(long id);

    Optional<String> findName(long id);

    /** Newest profile that isn't archived - the allocation preview's sample recipient. */
    Optional<String> findNewestActiveName();

    /** Insert the profile and its category links in one transaction. */
    long insert(Profile profile);

    /** Update the profile and its category links in one transaction, unless the row is archived. */
    boolean updateUnlessArchived(Profile profile);

    /** False when no row has that id. */
    boolean archive(long id);

    SidebarStats sidebarStats();
}
