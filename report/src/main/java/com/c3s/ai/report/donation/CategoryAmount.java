package com.c3s.ai.report.donation;

/** Gifts to a category's profiles in a period; a profile in two categories counts in both. */
public record CategoryAmount(String label, String emoji, String colorClass, long amount) {
}
