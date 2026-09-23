package com.c3s.ai.admin.allocation;

import com.c3s.ai.common.util.Money;

import java.util.ArrayList;
import java.util.List;

/**
 * The "how your donation is used" sentence, as values rather than markup. Mirrored in
 * js/allocation-form.js, which fills the same slots while the form is edited - keep in sync.
 */
public final class AllocationMessageBuilder {

    private AllocationMessageBuilder() {
    }

    public static DonorMessage build(String recipientName, List<AllocationBucket> buckets,
                                     AllocationBreakdown breakdown) {
        if (buckets.isEmpty() || breakdown.buckets().isEmpty()) {
            return DonorMessage.EMPTY;
        }

        // AllocationValidator enforces exactly one to-recipient bucket, but this also runs on
        // the pre-validation render, so fall back to the first bucket if none is marked.
        int recipientIndex = 0;
        for (int i = 0; i < buckets.size(); i++) {
            if (buckets.get(i).isToRecipient()) {
                recipientIndex = i;
                break;
            }
        }
        AllocationBucket recipientBucket = buckets.get(recipientIndex);
        AllocationBreakdown.BucketAmount recipientAmount = breakdown.buckets().get(recipientIndex);
        String who = (recipientName == null || recipientName.isBlank()) ? "the recipient" : recipientName;

        boolean deducted = breakdown.feeModel() == FeeModel.DEDUCTED;
        String feeAmount = deducted && breakdown.processing() > 0 ? money(breakdown.processing()) : null;
        String giftRef = deducted ? "your gift" : "your " + money(breakdown.gift()) + " gift";

        List<DonorMessage.Part> others = new ArrayList<>();
        for (int i = 0; i < buckets.size(); i++) {
            if (i == recipientIndex || buckets.get(i).getPct() <= 0) {
                continue;
            }
            AllocationBucket b = buckets.get(i);
            others.add(new DonorMessage.Part(b.getPct(), money(breakdown.buckets().get(i).amount()),
                    b.getEffectivePurpose()));
        }

        return new DonorMessage(feeAmount, recipientBucket.getPct(), money(recipientAmount.amount()),
                giftRef, who, others);
    }

    private static String money(double amount) {
        return Money.money(amount);
    }
}
