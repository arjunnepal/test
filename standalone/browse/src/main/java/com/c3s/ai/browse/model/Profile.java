package com.c3s.ai.browse.model;

import java.util.ArrayList;
import java.util.List;

public class Profile {

    private Long id;
    private String name;
    private String blurb;
    private String emoji;
    private Integer goal;
    private Integer raised;
    private List<Category> categories = new ArrayList<>();

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

    public String getBlurb() {
        return blurb;
    }

    public void setBlurb(String blurb) {
        this.blurb = blurb;
    }

    // falls back to the category emoji
    public String getDisplayEmoji() {
        if (emoji != null && !emoji.isBlank()) {
            return emoji;
        }
        Category first = getFirstCategory();
        return first != null ? first.getEmoji() : null;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
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

    public List<Category> getCategories() {
        return categories;
    }

    public void setCategories(List<Category> categories) {
        this.categories = categories != null ? categories : new ArrayList<>();
    }

    public Category getFirstCategory() {
        return categories.isEmpty() ? null : categories.get(0);
    }

    public int getPercentRaised() {
        if (goal == null || goal <= 0 || raised == null) {
            return 0;
        }
        // capped at 100
        return Math.min(100, Math.round(100f * raised / goal));
    }
}
