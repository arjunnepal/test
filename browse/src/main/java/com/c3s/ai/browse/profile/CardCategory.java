package com.c3s.ai.browse.profile;

/** One of a published profile's categories, as a card needs it. Active categories only. */
public record CardCategory(long id, String label, String emoji, String colorClass) {
}
