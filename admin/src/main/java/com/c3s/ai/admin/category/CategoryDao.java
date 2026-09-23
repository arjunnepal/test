package com.c3s.ai.admin.category;

import java.util.List;
import java.util.Optional;

/** Package-private: every category read and write goes through CategoryService and its rules. */
interface CategoryDao {

    /** Every category, active and archived, by label. */
    List<Category> findAll();

    /** Active categories by label - the profile form's picker and the allocation list. */
    List<CategoryRef> findActiveRefs();

    Optional<Category> findById(long id);

    Optional<String> findLabel(long id);

    /** excludeId = the row being edited, so it doesn't clash with itself. null when creating. */
    boolean labelExists(String label, Long excludeId);

    long insert(Category category);

    /** Updates label, emoji and colour only while the row is still active. False when no active row matched. */
    boolean updateActive(Category category);

    /** False when no row has that id. */
    boolean archive(long id);
}
