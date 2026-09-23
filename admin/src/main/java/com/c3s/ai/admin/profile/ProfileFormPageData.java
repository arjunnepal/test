package com.c3s.ai.admin.profile;

import com.c3s.ai.admin.category.CategoryRef;
import com.c3s.ai.core.web.FormPageData;

import java.util.List;

/** Page object for profile-form.jsp */
public class ProfileFormPageData extends FormPageData {

    private final Profile profile;
    private final List<CategoryRef> categories;
    private final String pendingPhoto;

    public ProfileFormPageData(Profile profile, List<CategoryRef> categories, String pendingPhoto) {
        this.profile = profile;
        this.categories = List.copyOf(categories);
        this.pendingPhoto = pendingPhoto;
    }

    public Profile getProfile() {
        return profile;
    }

    /** The picker: active categories. */
    public List<CategoryRef> getCategories() {
        return categories;
    }

    /** A photo uploaded on an earlier failed submit, carried in a hidden field. */
    public String getPendingPhoto() {
        return pendingPhoto;
    }

    public boolean isSelected(long categoryId) {
        return profile.getCategories().stream().anyMatch(c -> c.id() == categoryId);
    }
}
