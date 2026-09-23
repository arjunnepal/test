package com.c3s.ai.browse.page;

/**
 * Maps admin's color_class to the giving page's look: the tag and chip classes, and the colours
 * set inline on the card background, category bar and amount.
 */
public final class BrowseStyle {

    private final String tagClass;
    private final String chipClass;
    private final String accentColor;
    private final String cardBackground;

    private BrowseStyle(String tagClass, String chipClass, String accentColor, String cardBackground) {
        this.tagClass = tagClass;
        this.chipClass = chipClass;
        this.accentColor = accentColor;
        this.cardBackground = cardBackground;
    }

    public String getTagClass() {
        return tagClass;
    }

    public String getChipClass() {
        return chipClass;
    }

    public String getAccentColor() {
        return accentColor;
    }

    public String getCardBackground() {
        return cardBackground;
    }

    private static final BrowseStyle BLUE = new BrowseStyle("te", "cat-ed",
            "var(--uw-blue)", "linear-gradient(135deg,var(--uw-blue-light),#b3cef0)");
    private static final BrowseStyle RED = new BrowseStyle("th", "cat-he",
            "var(--uw-red)", "linear-gradient(135deg,var(--uw-red-light),#f5b3b3)");
    private static final BrowseStyle GOLD = new BrowseStyle("tho", "cat-ho",
            "#92620a", "linear-gradient(135deg,var(--uw-gold-light),#fde4a0)");
    private static final BrowseStyle GREEN = new BrowseStyle("tf", "cat-fo",
            "var(--uw-green)", "linear-gradient(135deg,var(--uw-green-light),#a5d6a7)");
    private static final BrowseStyle PURPLE = new BrowseStyle("tv", "cat-ve",
            "var(--uw-purple)", "linear-gradient(135deg,#F0E8FF,#d4b8f0)");

    public static BrowseStyle forColorClass(String colorClass) {
        if (colorClass == null) {
            return BLUE;
        }
        return switch (colorClass) {
            case "cat-red" -> RED;
            case "cat-gold" -> GOLD;
            case "cat-green" -> GREEN;
            case "cat-purple" -> PURPLE;
            default -> BLUE;
        };
    }
}
