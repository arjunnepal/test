package com.c3s.ai.report.page;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The reference report's formats - js/report.js prints the same. */
class ReportFormatTest {

    private static final String GRAD_CAP = new String(Character.toChars(0x1F393));

    @Test
    void wholeDollarsRoundHalfUp() {
        assertEquals("$842,410", ReportFormat.money0(new BigDecimal("842409.50")));
        assertEquals("$0", ReportFormat.money0(BigDecimal.ZERO));
        assertEquals("$2,500", ReportFormat.money0(2_500));
    }

    @Test
    void centsOfEachDollar() {
        assertEquals("3.3¢", ReportFormat.perDollar(new BigDecimal("3.25"), new BigDecimal("100")));
        assertEquals("0.0¢", ReportFormat.perDollar(BigDecimal.ONE, BigDecimal.ZERO));
    }

    @Test
    void theFeeRateDropsTrailingZeros() {
        assertEquals("2.9", ReportFormat.rate(new BigDecimal("2.90")));
        assertEquals("3", ReportFormat.rate(new BigDecimal("3.00")));
        assertEquals("10", ReportFormat.rate(new BigDecimal("10.00")));
    }

    @Test
    void aDeductionUsesAMinusSignUnlessNothingWasTaken() {
        assertEquals("−$25", ReportFormat.deduction(new BigDecimal("24.80")));
        assertEquals("$0", ReportFormat.deduction(BigDecimal.ZERO));
    }

    @Test
    void weeklyBarsAreInThousands() {
        assertEquals("$12.4K", ReportFormat.thousands(12_400));
        assertEquals("$0.3K", ReportFormat.thousands(250));
        assertEquals("15.00", ReportFormat.barPct(15));
        assertEquals("15.0%", ReportFormat.pct1(14.96));
    }

    @Test
    void onlyAWideEnoughSegmentIsLabelled() {
        assertEquals("13.0%", ReportFormat.segmentLabel(ReportFormat.MIN_LABELLED_SEGMENT_PCT));
        assertEquals("", ReportFormat.segmentLabel(12.9));
    }

    @Test
    void aCategoryReadsAsEmojiAndLabel() {
        assertEquals(GRAD_CAP + " Education", ReportFormat.category(GRAD_CAP, "Education"));
        assertEquals("Other", ReportFormat.category(null, "Other"));
        assertEquals("", ReportFormat.category(GRAD_CAP, null));
    }
}
