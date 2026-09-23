package com.c3s.ai.browse.category;

import java.util.List;

/** What the giving page may ask of uwnefl_categories. The DAO behind it is package-private. */
public interface CategoryQueries {

    static CategoryQueries create() {
        return new JdbcSiteCategoryDao();
    }

    /** Active categories with at least one published profile, in creation order (the chip strip). */
    List<ChipCategory> findInUse();
}
