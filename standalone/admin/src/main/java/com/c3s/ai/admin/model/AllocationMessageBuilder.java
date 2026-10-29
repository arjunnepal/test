package com.c3s.ai.admin.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// Builds the "how your donation is used" sentence for the settings-page preview.
// Mirrored in allocation-preview.js - keep in sync.
public final class AllocationMessageBuilder {

    private AllocationMessageBuilder() {
    }

    public static String build(String recipientName, List<AllocationBucket> buckets, AllocationBreakdown breakdown) {
        if (buckets.isEmpty() || breakdown.buckets().isEmpty()) {
            return "";
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
        // this string is rendered as raw HTML (the <strong> tags), so escape anything admin-entered
        String who = escapeForMessage(
                (recipientName == null || recipientName.isBlank()) ? "the recipient" : recipientName);

        boolean deducted = breakdown.feeModel() == FeeModel.DEDUCTED;

        StringBuilder msg = new StringBuilder();
        if (deducted && breakdown.processing() > 0) {
            msg.append("After the ").append(money(breakdown.processing())).append(" processing fee, ");
        }
        String giftRef = deducted ? "your gift" : "your " + money(breakdown.gift()) + " gift";
        msg.append("<strong>").append(recipientBucket.getPct()).append("%</strong> of ").append(giftRef)
                .append(" — <strong>").append(money(recipientAmount.amount()))
                .append("</strong> — goes directly to support ").append(who);

        List<String> others = new ArrayList<>();
        for (int i = 0; i < buckets.size(); i++) {
            if (i == recipientIndex || buckets.get(i).getPct() <= 0) {
                continue;
            }
            AllocationBucket b = buckets.get(i);
            AllocationBreakdown.BucketAmount amt = breakdown.buckets().get(i);
            others.add("<strong>" + b.getPct() + "%</strong> — <strong>" + money(amt.amount())
                    + "</strong> — supports " + escapeForMessage(b.getEffectivePurpose()));
        }

        if (others.isEmpty()) {
            return msg.append(".").toString();
        }
        return msg.append(", and ").append(String.join("; and ", others)).append(".").toString();
    }

    private static String money(double amount) {
        return String.format(Locale.US, "$%,.2f", amount);
    }

    private static String escapeForMessage(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
