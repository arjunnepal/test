package com.c3s.ai.admin.allocation;

import com.c3s.ai.common.db.TestDatabase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

// Skipped unless DONATION_IT_JDBC_URL is set (see TestDatabase).
class AllocationDaoIT {

    private final JdbcAllocationDao dao = new JdbcAllocationDao();

    @BeforeAll
    static void database() throws Exception {
        assumeTrue(TestDatabase.start(), "DONATION_IT_JDBC_URL not set - skipping DB integration tests");
    }

    @Test
    void theSeededDefaultPolicyLoadsWithItsCoreBuckets() {
        AllocationSettings defaults = dao.findDefault().orElseThrow();
        List<AllocationBucket> buckets = dao.findBuckets(defaults.getId());
        assertEquals(List.of("restricted", "unrestricted"), buckets.stream().map(AllocationBucket::getBucketKey).toList());
        assertTrue(buckets.get(0).isToRecipient() && buckets.get(0).isCore());
    }

    @Test
    void anOverrideIsInsertedThenUpdatedInOneTransactionEach() {
        AllocationSettings override = new AllocationSettings();
        override.setCategoryId(5L);
        override.setFeePct(new BigDecimal("3.10"));
        AllocationBucket all = new AllocationBucket();
        all.setBucketKey("restricted");
        all.setName("Everything");
        all.setPct(100);
        all.setToRecipient(true);
        all.setCore(true);

        long id = dao.saveSettingsAndBuckets(override, List.of(all)).orElseThrow();
        assertTrue(dao.findOverrideCategoryIds().contains(5L));
        assertEquals(0, new BigDecimal("3.10").compareTo(dao.findOverride(5L).orElseThrow().getFeePct()));

        override.setId(id);
        override.setFeeModel(FeeModel.ABSORBED);
        all.setName("Renamed");
        dao.saveSettingsAndBuckets(override, List.of(all)).orElseThrow();
        assertEquals(FeeModel.ABSORBED, dao.findOverride(5L).orElseThrow().getFeeModel());
        assertEquals("Renamed", dao.findBuckets(id).get(0).getName());

        override.setId(999_999L);
        assertTrue(dao.saveSettingsAndBuckets(override, List.of(all)).isEmpty());
    }
}
