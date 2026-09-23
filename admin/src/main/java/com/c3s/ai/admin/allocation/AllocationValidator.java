package com.c3s.ai.admin.allocation;

import com.c3s.ai.common.util.Strings;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Server-side allocation-policy validation - shares total 100%, every bucket named, fee in range. */
final class AllocationValidator {

    static final int MAX_BUCKETS = 6;
    private static final BigDecimal FEE_PCT_MAX = new BigDecimal("10");
    private static final BigDecimal FEE_FIXED_MAX = new BigDecimal("5");

    private AllocationValidator() {
    }

    /** Buckets here must already carry the server's own core/to-recipient flags (see AllocationService). */
    static Map<String, String> validate(List<AllocationBucket> buckets, AllocationSettings settings) {
        Map<String, String> errors = new LinkedHashMap<>();

        if (buckets == null || buckets.isEmpty()) {
            errors.put("buckets", "At least one allocation bucket is required.");
            return errors;
        }

        int total = buckets.stream().mapToInt(AllocationBucket::getPct).sum();
        if (total != 100) {
            errors.put("bucketsTotal", "Allocations total " + total + "% - they must total 100%.");
        }

        if (buckets.stream().anyMatch(b -> Strings.isBlank(b.getName()))) {
            errors.put("bucketsName", "Every allocation bucket needs a name.");
        } else if (buckets.stream().anyMatch(b -> Strings.longerThan(b.getName(), AllocationBucket.NAME_MAX))) {
            errors.put("bucketsName", "Bucket names must be " + AllocationBucket.NAME_MAX + " characters or fewer.");
        }

        if (buckets.stream().anyMatch(b -> b.getPct() < 0 || b.getPct() > 100)) {
            errors.put("bucketsRange", "Bucket shares must be between 0 and 100.");
        }

        List<String> structural = new ArrayList<>();
        if (buckets.size() > MAX_BUCKETS) {
            structural.add("No more than " + MAX_BUCKETS + " allocation buckets.");
        }
        Set<String> keys = new HashSet<>();
        for (AllocationBucket b : buckets) {
            if (Strings.isBlank(b.getBucketKey()) || Strings.longerThan(b.getBucketKey(), AllocationBucket.KEY_MAX)) {
                structural.add("A bucket is missing its identifier - reload the page and try again.");
                break;
            }
            if (!keys.add(b.getBucketKey())) {
                structural.add("Two buckets share an identifier - reload the page and try again.");
                break;
            }
        }
        if (buckets.stream().anyMatch(b -> Strings.longerThan(b.getPurpose(), AllocationBucket.TEXT_MAX))) {
            structural.add("Bucket descriptions must be " + AllocationBucket.TEXT_MAX + " characters or fewer.");
        }
        if (buckets.stream().filter(AllocationBucket::isToRecipient).count() != 1) {
            structural.add("Exactly one bucket must be marked as going to the recipient.");
        }
        if (buckets.stream().noneMatch(AllocationBucket::isCore)) {
            structural.add("At least one core bucket (Restricted/Unrestricted) is required.");
        }
        if (!structural.isEmpty()) {
            errors.merge("buckets", String.join(" ", structural), (a, b) -> a + " " + b);
        }

        if (settings.getFeePct() == null
                || settings.getFeePct().signum() < 0
                || settings.getFeePct().compareTo(FEE_PCT_MAX) > 0) {
            errors.put("feePct", "Processor rate must be between 0% and " + FEE_PCT_MAX.toPlainString() + "%.");
        }

        if (settings.getFeeFixed() == null
                || settings.getFeeFixed().signum() < 0
                || settings.getFeeFixed().compareTo(FEE_FIXED_MAX) > 0) {
            errors.put("feeFixed", "Fixed fee must be between $0 and $" + FEE_FIXED_MAX.toPlainString() + ".");
        }

        return errors;
    }
}
