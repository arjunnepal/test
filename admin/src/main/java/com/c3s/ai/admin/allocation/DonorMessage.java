package com.c3s.ai.admin.allocation;

import java.util.List;

/**
 * The "how your donation is used" sentence as values, for allocation-preview.jsp to render.
 * Nothing here is escaped - whoever renders it escapes it.
 */
public record DonorMessage(String feeAmount, int recipientPct, String recipientAmount, String giftRef,
                           String who, List<Part> others) {

    /** No buckets yet, so there's no sentence to show. */
    public static final DonorMessage EMPTY = new DonorMessage(null, 0, null, null, null, List.of());

    public DonorMessage {
        others = List.copyOf(others);
    }

    public boolean isEmpty() {
        return recipientAmount == null;
    }

    /** Set only when the processing fee comes out of the gift. */
    public boolean hasFee() {
        return feeAmount != null;
    }

    /** One of the "and x% - $y - supports z" clauses after the recipient's share. */
    public record Part(int pct, String amount, String purpose) {
    }
}
