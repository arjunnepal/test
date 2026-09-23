package com.c3s.ai.admin.category;

import java.util.List;

/** An admin-managed profile category, as the category list and form work with it. */
public class Category {

    // fixed set - enforced by CategoryValidator and a CHECK constraint
    public static final List<String> COLOR_CLASSES =
            List.of("cat-blue", "cat-red", "cat-gold", "cat-green", "cat-purple", "cat-neutral");

    public static final String DEFAULT_COLOR_CLASS = COLOR_CLASSES.get(0);

    // column sizes in schema.sql
    static final int LABEL_MAX = 100;
    static final int EMOJI_MAX = 8;

    private Long id;
    private String label;
    private String emoji;
    private String colorClass = DEFAULT_COLOR_CLASS;
    private CategoryStatus status = CategoryStatus.ACTIVE;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public String getColorClass() {
        return colorClass;
    }

    public void setColorClass(String colorClass) {
        this.colorClass = colorClass;
    }

    public CategoryStatus getStatus() {
        return status;
    }

    public void setStatus(CategoryStatus status) {
        this.status = status;
    }

    public boolean isActive() {
        return status == CategoryStatus.ACTIVE;
    }
}
