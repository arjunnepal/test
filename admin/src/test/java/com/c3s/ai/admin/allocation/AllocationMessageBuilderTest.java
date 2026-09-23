package com.c3s.ai.admin.allocation;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// The donor sentence is values, not markup - allocation-preview.jsp renders and escapes them,
// and js/allocation-form.js fills the same slots. Mirrored there.
class AllocationMessageBuilderTest {

    private static AllocationBucket bucket(String name, int pct, boolean toRecipient) {
        AllocationBucket b = new AllocationBucket();
        b.setBucketKey(name.toLowerCase());
        b.setName(name);
        b.setPct(pct);
        b.setToRecipient(toRecipient);
        return b;
    }

    private static AllocationSettings settings(FeeModel model, String pct, String fixed) {
        AllocationSettings s = new AllocationSettings();
        s.setFeeModel(model);
        s.setFeePct(new BigDecimal(pct));
        s.setFeeFixed(new BigDecimal(fixed));
        return s;
    }

    private static DonorMessage build(String name, AllocationSettings settings, List<AllocationBucket> buckets) {
        return AllocationMessageBuilder.build(name, buckets, AllocationCalculator.compute(50, settings, buckets));
    }

    @Test
    void noBucketsMeansNoSentence() {
        DonorMessage message = build("Marcus T.", settings(FeeModel.DEDUCTED, "2.9", "0.30"), List.of());
        assertTrue(message.isEmpty());
        assertEquals(DonorMessage.EMPTY, message);
    }

    @Test
    void aDeductedFeeOpensTheSentenceAndTheGiftIsReferredToPlainly() {
        DonorMessage message = build("Marcus T.", settings(FeeModel.DEDUCTED, "2.9", "0.30"),
                List.of(bucket("Restricted", 85, true), bucket("Unrestricted", 15, false)));

        assertTrue(message.hasFee());
        assertEquals("$1.75", message.feeAmount());
        assertEquals("your gift", message.giftRef());
        assertEquals(85, message.recipientPct());
        assertEquals("$41.01", message.recipientAmount());
        assertEquals("Marcus T.", message.who());
    }

    @Test
    void anAbsorbedFeeIsNotMentionedAndTheGiftIsNamed() {
        DonorMessage message = build("Marcus T.", settings(FeeModel.ABSORBED, "2.9", "0.30"),
                List.of(bucket("Restricted", 100, true)));

        assertFalse(message.hasFee());
        assertNull(message.feeAmount());
        assertEquals("your $50.00 gift", message.giftRef());
        assertEquals(List.of(), message.others());
    }

    @Test
    void aZeroFeeIsNotMentionedEvenWhenItIsDeducted() {
        DonorMessage message = build("Marcus T.", settings(FeeModel.DEDUCTED, "0", "0"),
                List.of(bucket("Restricted", 100, true)));
        assertFalse(message.hasFee());
    }

    @Test
    void everyOtherBucketWithAShareBecomesItsOwnClause() {
        DonorMessage message = build("Marcus T.", settings(FeeModel.ABSORBED, "0", "0"),
                List.of(bucket("Restricted", 60, true), bucket("Unrestricted", 30, false),
                        bucket("Admin", 10, false), bucket("Emergency", 0, false)));

        assertEquals(List.of("Unrestricted", "Admin"), message.others().stream().map(DonorMessage.Part::purpose).toList());
        assertEquals(List.of(30, 10), message.others().stream().map(DonorMessage.Part::pct).toList());
        assertEquals(List.of("$15.00", "$5.00"), message.others().stream().map(DonorMessage.Part::amount).toList());
    }

    @Test
    void aClauseUsesThePurposeThenTheDescriptionThenTheName() {
        AllocationBucket withPurpose = bucket("Admin", 20, false);
        withPurpose.setPurpose("keeping the lights on");
        withPurpose.setDescription("stored description");

        AllocationBucket withDescription = bucket("Emergency", 20, false);
        withDescription.setDescription("sudden need");

        DonorMessage message = build("Marcus T.", settings(FeeModel.ABSORBED, "0", "0"),
                List.of(bucket("Restricted", 60, true), withPurpose, withDescription));

        assertEquals(List.of("keeping the lights on", "sudden need"),
                message.others().stream().map(DonorMessage.Part::purpose).toList());
    }

    @Test
    void anUnnamedRecipientIsCalledTheRecipientAndNothingIsEscapedHere() {
        DonorMessage blank = build("  ", settings(FeeModel.ABSORBED, "0", "0"), List.of(bucket("Restricted", 100, true)));
        assertEquals("the recipient", blank.who());

        DonorMessage named = build("Rosa & Miguel D.", settings(FeeModel.ABSORBED, "0", "0"),
                List.of(bucket("Restricted", 100, true)));
        assertEquals("Rosa & Miguel D.", named.who());
    }
}
