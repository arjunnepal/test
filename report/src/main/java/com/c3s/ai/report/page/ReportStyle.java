package com.c3s.ai.report.page;

/**
 * Maps admin's color_class to the report's look: the tag class on a top donor's category badge
 * and the colour set inline on a category bar and amount. The classes are the reference report's
 * (css/report.css), which has none for the neutral colour, so that one gets no class.
 */
public final class ReportStyle {

    private final String tagClass;
    private final String accentColor;

    private ReportStyle(String tagClass, String accentColor) {
        this.tagClass = tagClass;
        this.accentColor = accentColor;
    }

    public String getTagClass() {
        return tagClass;
    }

    public String getAccentColor() {
        return accentColor;
    }

    private static final ReportStyle BLUE = new ReportStyle("te", "var(--uw-blue)");
    private static final ReportStyle RED = new ReportStyle("th", "var(--uw-red)");
    private static final ReportStyle GOLD = new ReportStyle("tho", "#92620a");
    private static final ReportStyle GREEN = new ReportStyle("tf", "var(--uw-green)");
    private static final ReportStyle PURPLE = new ReportStyle("tv", "var(--uw-purple)");
    private static final ReportStyle NEUTRAL = new ReportStyle("", "var(--muted)");

    // unknown or missing (a profile with no category) reads as neutral
    public static ReportStyle forColorClass(String colorClass) {
        if (colorClass == null) {
            return NEUTRAL;
        }
        return switch (colorClass) {
            case "cat-blue" -> BLUE;
            case "cat-red" -> RED;
            case "cat-gold" -> GOLD;
            case "cat-green" -> GREEN;
            case "cat-purple" -> PURPLE;
            default -> NEUTRAL;
        };
    }
}
