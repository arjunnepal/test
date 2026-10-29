package com.c3s.ai.admin.validation;

import com.c3s.ai.admin.model.AllocationBucket;
import com.c3s.ai.admin.model.AllocationSettings;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Server-side allocation-policy validation - shares total 100%, every bucket named, fee in range. */
public final class AllocationValidator {

    private static final BigDecimal FEE_PCT_MAX = new BigDecimal("10");
    private static final BigDecimal FEE_FIXED_MAX = new BigDecimal("5");

    private AllocationValidator() {
    }

    public static Map<String, String> validate(List<AllocationBucket> buckets, AllocationSettings settings) {
        Map<String, String> errors = new LinkedHashMap<>();

        if (buckets == null || buckets.isEmpty()) {
            errors.put("buckets", "At least one allocation bucket is required.");
            return errors;
        }

        int total = buckets.stream().mapToInt(AllocationBucket::getPct).sum();
        if (total != 100) {
            errors.put("bucketsTotal", "Allocations total " + total + "% - they must total 100%.");
        }

        boolean unnamed = buckets.stream().anyMatch(b -> b.getName() == null || b.getName().isBlank());
        if (unnamed) {
            errors.put("bucketsName", "Every allocation bucket needs a name.");
        }

        boolean outOfRange = buckets.stream().anyMatch(b -> b.getPct() < 0 || b.getPct() > 100);
        if (outOfRange) {
            errors.put("bucketsRange", "Bucket shares must be between 0 and 100.");
        }

        // invariants the client's hidden core/to-recipient fields can't be trusted for.
        // both can fail at once, so join them into one message.
        List<String> structural = new ArrayList<>();
        long recipientCount = buckets.stream().filter(AllocationBucket::isToRecipient).count();
        if (recipientCount != 1) {
            structural.add("Exactly one bucket must be marked as going to the recipient.");
        }
        boolean hasCore = buckets.stream().anyMatch(AllocationBucket::isCore);
        if (!hasCore) {
            structural.add("At least one core bucket (Restricted/Unrestricted) is required.");
        }
        if (!structural.isEmpty()) {
            errors.put("buckets", String.join(" ", structural));
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
