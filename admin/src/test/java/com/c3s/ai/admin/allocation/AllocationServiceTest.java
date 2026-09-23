package com.c3s.ai.admin.allocation;

import com.c3s.ai.admin.category.CategoryRef;
import com.c3s.ai.common.exception.EntityNotFoundException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AllocationServiceTest {

    private static final CategoryRef HEALTH = new CategoryRef(2, "Health", "H", "cat-red");

    private static AllocationSettings settings(Long id, Long categoryId) {
        AllocationSettings s = new AllocationSettings();
        s.setId(id);
        s.setCategoryId(categoryId);
        return s;
    }

    private static AllocationBucket bucket(String key, String name, int pct, boolean toRecipient, boolean core) {
        AllocationBucket b = new AllocationBucket();
        b.setBucketKey(key);
        b.setName(name);
        b.setPct(pct);
        b.setToRecipient(toRecipient);
        b.setCore(core);
        b.setDescription("stored description of " + key);
        return b;
    }

    /** The seeded default: restricted (recipient, core) 85% + unrestricted (core) 15%. */
    private static final class FakeDao implements AllocationDao {
        final Map<Long, AllocationSettings> overrides = new HashMap<>();
        final Map<Long, List<AllocationBucket>> buckets = new HashMap<>();
        List<AllocationBucket> saved;

        FakeDao() {
            buckets.put(1L, List.of(bucket("restricted", "Restricted", 85, true, true),
                    bucket("unrestricted", "Unrestricted", 15, false, true)));
        }

        public Optional<AllocationSettings> findDefault() {
            return Optional.of(settings(1L, null));
        }

        public Optional<AllocationSettings> findOverride(long categoryId) {
            return Optional.ofNullable(overrides.get(categoryId));
        }

        public List<AllocationBucket> findBuckets(long settingsId) {
            return buckets.getOrDefault(settingsId, List.of());
        }

        public Set<Long> findOverrideCategoryIds() {
            return overrides.keySet();
        }

        public OptionalLong saveSettingsAndBuckets(AllocationSettings s, List<AllocationBucket> b) {
            saved = b;
            return OptionalLong.of(s.getId() == null ? 77 : s.getId());
        }
    }

    private final FakeDao dao = new FakeDao();
    private final AllocationService service = new AllocationService(dao,
            id -> {
                if (id != HEALTH.id()) {
                    throw new EntityNotFoundException("category", id);
                }
                return HEALTH;
            },
            () -> List.of(HEALTH), () -> "Marcus T.");

    /** What the browser sends: flags and descriptions are whatever the client says. */
    private static AllocationBucket submitted(String key, String name, int pct, boolean claimsRecipient, boolean claimsCore) {
        AllocationBucket b = bucket(key, name, pct, claimsRecipient, claimsCore);
        b.setDescription("<b>client description</b>");
        return b;
    }

    private static AllocationSettings submittedSettings() {
        AllocationSettings s = new AllocationSettings();
        s.setFeePct(new BigDecimal("2.9"));
        s.setFeeFixed(new BigDecimal("0.30"));
        return s;
    }

    @Test
    void theStoredStructureWinsOverWhatTheClientClaims() throws Exception {
        List<AllocationBucket> sent = new ArrayList<>(List.of(
                submitted("restricted", "Restricted", 70, false, false),
                submitted("unrestricted", "Unrestricted", 20, true, false),
                submitted("custom-1-2", "Pantry", 10, false, true)));

        AllocationService.Submission s = service.prepare(null, submittedSettings(), sent, Map.of());

        assertTrue(s.errors().isEmpty(), s.errors().toString());
        AllocationBucket restricted = s.buckets().get(0);
        AllocationBucket unrestricted = s.buckets().get(1);
        AllocationBucket pantry = s.buckets().get(2);
        assertTrue(restricted.isToRecipient() && restricted.isCore());
        assertFalse(unrestricted.isToRecipient());
        assertTrue(unrestricted.isCore());
        assertFalse(pantry.isCore() || pantry.isToRecipient(), "a new bucket can't make itself core");
        assertEquals("stored description of restricted", restricted.getDescription());
        assertNull(pantry.getDescription());
        assertEquals(1L, s.settings().getId(), "the default policy's row, resolved server-side");
    }

    @Test
    void aCoreBucketCannotBeDropped() throws Exception {
        List<AllocationBucket> sent = List.of(
                submitted("restricted", "Restricted", 90, true, true),
                submitted("custom-1-2", "Pantry", 10, false, false));

        AllocationService.Submission s = service.prepare(null, submittedSettings(), sent, Map.of());

        assertTrue(s.errors().get("buckets").contains(AllocationService.CORE_REMOVED));
    }

    @Test
    void bucketKeysMustBePresentAndUnique() throws Exception {
        List<AllocationBucket> duplicate = List.of(
                submitted("restricted", "Restricted", 50, true, true),
                submitted("unrestricted", "Unrestricted", 25, false, true),
                submitted("unrestricted", "Again", 25, false, false));
        assertTrue(service.prepare(null, submittedSettings(), duplicate, Map.of()).errors().get("buckets").contains("share an identifier"));

        List<AllocationBucket> missing = List.of(
                submitted("restricted", "Restricted", 50, true, true),
                submitted("unrestricted", "Unrestricted", 25, false, true),
                submitted(null, "No key", 25, false, false));
        assertTrue(service.prepare(null, submittedSettings(), missing, Map.of()).errors().get("buckets").contains("missing its identifier"));
    }

    @Test
    void noMoreThanSixBuckets() throws Exception {
        List<AllocationBucket> sent = new ArrayList<>(List.of(
                submitted("restricted", "Restricted", 50, true, true),
                submitted("unrestricted", "Unrestricted", 20, false, true)));
        for (int i = 0; i < 5; i++) {
            sent.add(submitted("custom-" + i, "Extra " + i, i == 4 ? 10 : 5, false, false));
        }
        assertTrue(service.prepare(null, submittedSettings(), sent, Map.of()).errors().get("buckets").contains("No more than 6"));
    }

    @Test
    void aCategorysFirstPolicyIsHeldToTheDefaultsStructureAndInsertsANewRow() throws Exception {
        List<AllocationBucket> sent = List.of(
                submitted("restricted", "Restricted", 85, false, false),
                submitted("unrestricted", "Unrestricted", 15, false, false));

        AllocationService.Submission s = service.prepare(HEALTH.id(), submittedSettings(), sent, Map.of());

        assertTrue(s.errors().isEmpty(), s.errors().toString());
        assertTrue(s.newOverride());
        assertNull(s.settings().getId(), "no row yet - the save inserts one");
        assertEquals(HEALTH.id(), s.settings().getCategoryId());
        assertTrue(s.buckets().get(0).isToRecipient());
        assertEquals(77L, service.save(s));
    }

    @Test
    void anArchivedOrUnknownCategoryCannotHaveAPolicy() {
        assertThrows(EntityNotFoundException.class, () -> service.loadForm(9L));
        assertThrows(EntityNotFoundException.class,
                () -> service.prepare(9L, submittedSettings(), List.of(), Map.of()));
    }

    @Test
    void aSubmissionWithErrorsIsNeverSaved() throws Exception {
        AllocationService.Submission bad = service.prepare(null, submittedSettings(),
                List.of(submitted("restricted", "Restricted", 60, true, true)), Map.of());
        assertFalse(bad.errors().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> service.save(bad));
        assertNull(dao.saved);
    }

    @Test
    void theSampleNameIsTheCategoryElseTheNewestProfile() {
        assertEquals("Health", service.sampleName(HEALTH));
        assertEquals("Marcus T.", service.sampleName(null));
    }
}
