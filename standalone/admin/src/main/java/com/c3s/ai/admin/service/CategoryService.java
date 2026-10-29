package com.c3s.ai.admin.service;

import com.c3s.ai.admin.dao.CategoryDao;
import com.c3s.ai.admin.dao.JdbcCategoryDao;
import com.c3s.ai.admin.exception.EntityNotFoundException;
import com.c3s.ai.admin.model.Category;
import com.c3s.ai.admin.validation.CategoryValidator;

import java.util.List;
import java.util.Map;

/** Category use cases. */
public class CategoryService {

    private final CategoryDao categoryDao;

    public CategoryService() {
        this(new JdbcCategoryDao());
    }

    public CategoryService(CategoryDao categoryDao) {
        this.categoryDao = categoryDao;
    }

    public List<Category> listAll() {
        return categoryDao.findAll(false);
    }

    public List<Category> listActive() {
        return categoryDao.findAll(true);
    }

    public Category get(long id) throws EntityNotFoundException {
        return categoryDao.findById(id);
    }

    /** Field rules + label uniqueness (hits the DB). */
    public Map<String, String> validate(Category category) {
        return CategoryValidator.validate(category, categoryDao);
    }

    public long create(Category category) {
        return categoryDao.insert(category);
    }

    public void update(Category category) throws EntityNotFoundException {
        categoryDao.update(category);
    }

    /** Returns the label. */
    public String archive(long id) throws EntityNotFoundException {
        String label = categoryDao.findById(id).getLabel();
        categoryDao.archive(id);
        return label;
    }
}
