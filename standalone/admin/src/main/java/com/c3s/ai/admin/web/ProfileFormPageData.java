package com.c3s.ai.admin.web;

import com.c3s.ai.admin.model.Category;
import com.c3s.ai.admin.model.Profile;

import java.util.List;
import java.util.Set;

// page object for profile-form.jsp
public class ProfileFormPageData extends FormPageData {

    private boolean edit;
    private Profile profile;
    private Set<Long> selectedCategoryIds;
    private List<Category> categories;
    private String pendingPhoto;

    public boolean isEdit() {
        return edit;
    }

    public void setEdit(boolean edit) {
        this.edit = edit;
    }

    public Profile getProfile() {
        return profile;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    public Set<Long> getSelectedCategoryIds() {
        return selectedCategoryIds;
    }

    public void setSelectedCategoryIds(Set<Long> selectedCategoryIds) {
        this.selectedCategoryIds = selectedCategoryIds;
    }

    public List<Category> getCategories() {
        return categories;
    }

    public void setCategories(List<Category> categories) {
        this.categories = categories;
    }

    public String getPendingPhoto() {
        return pendingPhoto;
    }

    public void setPendingPhoto(String pendingPhoto) {
        this.pendingPhoto = pendingPhoto;
    }

    public boolean isSelected(Long categoryId) {
        return selectedCategoryIds != null && selectedCategoryIds.contains(categoryId);
    }
}
