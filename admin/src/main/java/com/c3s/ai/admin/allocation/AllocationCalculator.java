package com.c3s.ai.admin.allocation;

import java.util.ArrayList;
import java.util.List;

/** Splits a sample donation across the fee and each bucket. Mirrored in js/allocation-form.js. */
public final class AllocationCalculator {

    private AllocationCalculator() {
    }

    public static AllocationBreakdown compute(double amount, AllocationSettings settings, List<AllocationBucket> buckets) {
        double gift = Math.max(0, amount);
        double fee = gift > 0
                ? round2(gift * (settings.getFeePct().doubleValue() / 100) + settings.getFeeFixed().doubleValue())
                : 0;

        double charge;
        double processing;
        double net;
        switch (settings.getFeeModel()) {
            case ADDED -> { charge = round2(gift + fee); processing = fee; net = gift; }
            case ABSORBED -> { charge = gift; processing = 0; net = gift; }
            default -> { charge = gift; processing = Math.min(fee, gift); net = round2(gift - processing); }
        }

        int total = buckets.stream().mapToInt(AllocationBucket::getPct).sum();
        int totalOrHundred = total == 0 ? 100 : total;

        List<AllocationBreakdown.BucketAmount> result = new ArrayList<>();
        double running = 0;
        for (int i = 0; i < buckets.size(); i++) {
            AllocationBucket b = buckets.get(i);
            boolean isLast = i == buckets.size() - 1;
            // last bucket takes the remainder so the parts always add back to net
            double amt = isLast ? round2(net - running) : round2(net * (b.getPct() / (double) totalOrHundred));
            running = round2(running + amt);
            double pctOfCharge = charge > 0 ? amt / charge * 100 : 0;
            result.add(new AllocationBreakdown.BucketAmount(b.getName(), b.getColorClass(), amt, pctOfCharge, b.getPct()));
        }

        return new AllocationBreakdown(gift, fee, charge, processing, net, settings.getFeeModel(), result);
    }

    private static double round2(double v) {
        return Math.round(v * 100) / 100.0;
    }
}
