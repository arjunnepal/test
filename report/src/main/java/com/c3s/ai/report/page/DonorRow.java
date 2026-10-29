package com.c3s.ai.report.page;

/**
 * A row of "Top Donors This Month". rankClass / rankColor give first place its gold and second and
 * third their colours, as the reference does; both are empty below that.
 */
public record DonorRow(int rank, String rankClass, String rankColor, String donor, String recipient,
                       String tagClass, String category, String amount, String type, String date) {
}
