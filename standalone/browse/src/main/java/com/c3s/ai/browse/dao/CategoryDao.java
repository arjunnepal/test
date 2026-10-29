package com.c3s.ai.browse.dao;

import com.c3s.ai.browse.model.Category;

import java.util.List;

public interface CategoryDao {

    // active categories that have at least one published profile
    List<Category> findInUse();
}
