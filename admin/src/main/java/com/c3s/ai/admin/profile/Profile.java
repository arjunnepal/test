package com.c3s.ai.admin.profile;

import com.c3s.ai.admin.category.CategoryRef;
import com.c3s.ai.common.util.ProfileMath;
import com.c3s.ai.common.util.Strings;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** A recipient profile in full, as the edit form reads and writes it. */
public class Profile {

    // column sizes in schema.sql
    static final int NAME_MAX = 100;
    static final int CITY_MAX = 100;
    static final int BLURB_MAX = 160;
    static final int STORY_MAX = 2000;
    static final int EMOJI_MAX = 8;

    private Long id;
    private String name;
    private String city;
    private String state;
    private List<CategoryRef> categories = new ArrayList<>();
    private String blurb;
    private String story;
    private String emoji;
    private String photoFilename;
    private Integer goal;
    private Integer raised;
    private LocalDate deadline;
    private boolean identityVerified;
    private boolean needsReviewed;
    private boolean paypalConfirmed;
    private boolean storyReviewed;
    private ProfileStatus status = ProfileStatus.DRAFT;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public List<CategoryRef> getCategories() {
        return categories;
    }

    public void setCategories(List<CategoryRef> categories) {
        this.categories = categories != null ? new ArrayList<>(categories) : new ArrayList<>();
    }

    /** Used for the emoji/colour fallback - there's no real "primary category". */
    public CategoryRef getFirstCategory() {
        return categories.isEmpty() ? null : categories.get(0);
    }

    public String getBlurb() {
        return blurb;
    }

    public void setBlurb(String blurb) {
        this.blurb = blurb;
    }

    public String getStory() {
        return story;
    }

    public void setStory(String story) {
        this.story = story;
    }

    /** The profile's own emoji, else its first category's. */
    public String getDisplayEmoji() {
        CategoryRef first = getFirstCategory();
        return ProfileMath.displayEmoji(emoji, first != null ? first.emoji() : null);
    }

    /** Just what the admin typed, no fallback (may be null/blank); this is what gets persisted. */
    public String getOwnEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public String getPhotoFilename() {
        return photoFilename;
    }

    public void setPhotoFilename(String photoFilename) {
        this.photoFilename = photoFilename;
    }

    public boolean hasPhoto() {
        return !Strings.isBlank(photoFilename);
    }

    public Integer getGoal() {
        return goal;
    }

    public void setGoal(Integer goal) {
        this.goal = goal;
    }

    public Integer getRaised() {
        return raised;
    }

    public void setRaised(Integer raised) {
        this.raised = raised;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public boolean isIdentityVerified() {
        return identityVerified;
    }

    public void setIdentityVerified(boolean identityVerified) {
        this.identityVerified = identityVerified;
    }

    public boolean isNeedsReviewed() {
        return needsReviewed;
    }

    public void setNeedsReviewed(boolean needsReviewed) {
        this.needsReviewed = needsReviewed;
    }

    public boolean isPaypalConfirmed() {
        return paypalConfirmed;
    }

    public void setPaypalConfirmed(boolean paypalConfirmed) {
        this.paypalConfirmed = paypalConfirmed;
    }

    public boolean isStoryReviewed() {
        return storyReviewed;
    }

    public void setStoryReviewed(boolean storyReviewed) {
        this.storyReviewed = storyReviewed;
    }

    public ProfileStatus getStatus() {
        return status;
    }

    public void setStatus(ProfileStatus status) {
        this.status = status;
    }

    public int getPercentRaised() {
        return ProfileMath.percentRaised(goal, raised);
    }

}
