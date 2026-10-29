package com.c3s.ai.admin.service;

import com.c3s.ai.admin.dao.CategoryDao;
import com.c3s.ai.admin.dao.JdbcCategoryDao;
import com.c3s.ai.admin.dao.JdbcProfileDao;
import com.c3s.ai.admin.dao.ProfileDao;
import com.c3s.ai.admin.exception.EntityNotFoundException;
import com.c3s.ai.admin.model.Category;
import com.c3s.ai.admin.model.Profile;

import java.util.List;

/**
 * Profile use cases. Also the one place SidebarStatsCache.invalidate() is called after
 * any profile change. No-arg ctor wires the JDBC DAOs; the other is for tests.
 */
public class ProfileService {

    private final ProfileDao profileDao;
    private final CategoryDao categoryDao;
    private final SidebarStatsCache sidebarStats;

    public ProfileService() {
        this(new JdbcProfileDao(), new JdbcCategoryDao(), SidebarStatsCache.INSTANCE);
    }

    public ProfileService(ProfileDao profileDao, CategoryDao categoryDao, SidebarStatsCache sidebarStats) {
        this.profileDao = profileDao;
        this.categoryDao = categoryDao;
        this.sidebarStats = sidebarStats;
    }

    public List<Profile> listAll() {
        return profileDao.findAll(false);
    }

    public Profile get(long id) throws EntityNotFoundException {
        return profileDao.findById(id);
    }

    public List<Category> pickerCategories() {
        return categoryDao.findAll(true);
    }

    /** Newest active profile's name for the allocation preview sample; null if none. */
    public String newestActiveName() {
        return profileDao.findFirst(true).map(Profile::getName).orElse(null);
    }

    public long create(Profile profile) {
        long id = profileDao.insert(profile);
        sidebarStats.invalidate();
        return id;
    }

    public void update(Profile profile) throws EntityNotFoundException {
        profileDao.update(profile);
        sidebarStats.invalidate();
    }

    /** Returns the name (read before the soft-delete). */
    public String archive(long id) throws EntityNotFoundException {
        String name = profileDao.findById(id).getName();
        profileDao.archive(id);
        sidebarStats.invalidate();
        return name;
    }
}
