package com.c3s.ai.admin.dao;

import com.c3s.ai.admin.exception.EntityNotFoundException;
import com.c3s.ai.admin.model.Category;

import java.util.List;

public interface CategoryDao {

    /** activeOnly=true drops archived categories (the profile-form picker). */
    List<Category> findAll(boolean activeOnly);

    Category findById(long id) throws EntityNotFoundException;

    /** excludeId = the row being edited, so it doesn't clash with itself. null when creating. */
    boolean labelExists(String label, Long excludeId);

    long insert(Category category);

    void update(Category category) throws EntityNotFoundException;

    void archive(long id) throws EntityNotFoundException;
}
