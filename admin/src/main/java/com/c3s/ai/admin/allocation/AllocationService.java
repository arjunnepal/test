package com.c3s.ai.admin.allocation;

import com.c3s.ai.admin.category.CategoryRef;
import com.c3s.ai.admin.category.CategoryService;
import com.c3s.ai.admin.profile.ProfileService;
import com.c3s.ai.common.exception.DataAccessException;
import com.c3s.ai.common.exception.EntityNotFoundException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Allocation-policy use cases. Nothing structural comes from the client: the settings row is
 * resolved from the category, a bucket's flags and description come from the stored bucket with
 * the same key, every stored core bucket must still be there, and only active categories can have
 * an override.
 */
public class AllocationService {

    static final String CORE_REMOVED = "Core buckets (Restricted/Unrestricted) can't be removed.";

    @FunctionalInterface
    interface ActiveCategoryLookup {
        CategoryRef find(long id) throws EntityNotFoundException;
    }

    /** A policy as its form shows it. newOverride: a category with no policy yet, pre-filled from the default. */
    public record Form(CategoryRef category, AllocationSettings settings, List<AllocationBucket> buckets,
                       boolean newOverride) {
    }

    /** A submitted policy after the server has taken over its structure; errors empty when it can be saved. */
    public record Submission(CategoryRef category, AllocationSettings settings, List<AllocationBucket> buckets,
                             boolean newOverride, Map<String, String> errors) {
    }

    private final AllocationDao dao;
    private final ActiveCategoryLookup activeCategory;
    private final Supplier<List<CategoryRef>> activeCategories;
    private final Supplier<String> newestProfileName;

    public AllocationService() {
        this(new JdbcAllocationDao(), new CategoryService());
    }

    private AllocationService(AllocationDao dao, CategoryService categories) {
        this(dao, categories::getActive, categories::activeOptions, new ProfileService()::newestActiveName);
    }

    AllocationService(AllocationDao dao, ActiveCategoryLookup activeCategory,
                      Supplier<List<CategoryRef>> activeCategories, Supplier<String> newestProfileName) {
        this.dao = dao;
        this.activeCategory = activeCategory;
        this.activeCategories = activeCategories;
        this.newestProfileName = newestProfileName;
    }

    public List<CategoryRef> activeCategories() {
        return activeCategories.get();
    }

    public Set<Long> overrideCategoryIds() {
        return dao.findOverrideCategoryIds();
    }

    /** The policy for a category (null = the default policy). */
    public Form loadForm(Long categoryId) throws EntityNotFoundException {
        AllocationSettings defaults = defaultSettings();
        if (categoryId == null) {
            return new Form(null, defaults, dao.findBuckets(defaults.getId()), false);
        }
        CategoryRef category = activeCategory.find(categoryId);
        AllocationSettings override = dao.findOverride(categoryId).orElse(null);
        if (override != null) {
            return new Form(category, override, dao.findBuckets(override.getId()), false);
        }
        return new Form(category, templateFrom(defaults, categoryId), dao.findBuckets(defaults.getId()), true);
    }

    /** Resolve the row the save hits, replace client-supplied structure with the stored one, validate. */
    public Submission prepare(Long categoryId, AllocationSettings submitted, List<AllocationBucket> submittedBuckets,
                              Map<String, String> bindingErrors) throws EntityNotFoundException {
        AllocationSettings defaults = defaultSettings();
        CategoryRef category = categoryId == null ? null : activeCategory.find(categoryId);

        AllocationSettings target = categoryId == null ? defaults : dao.findOverride(categoryId).orElse(null);
        boolean newOverride = categoryId != null && target == null;
        submitted.setCategoryId(categoryId);
        submitted.setId(target != null ? target.getId() : null);

        // a new override starts from the default's buckets, so that's the structure to hold it to
        List<AllocationBucket> stored = dao.findBuckets(target != null ? target.getId() : defaults.getId());
        Map<String, AllocationBucket> storedByKey = stored.stream()
                .collect(Collectors.toMap(AllocationBucket::getBucketKey, Function.identity()));

        List<AllocationBucket> trusted = new ArrayList<>(submittedBuckets.size());
        for (AllocationBucket b : submittedBuckets) {
            AllocationBucket original = b.getBucketKey() == null ? null : storedByKey.get(b.getBucketKey());
            b.setCore(original != null && original.isCore());
            b.setToRecipient(original != null && original.isToRecipient());
            b.setDescription(original != null ? original.getDescription() : null);
            trusted.add(b);
        }

        Map<String, String> errors = new LinkedHashMap<>(bindingErrors);
        AllocationValidator.validate(trusted, submitted).forEach(errors::putIfAbsent);

        Set<String> submittedKeys = trusted.stream().map(AllocationBucket::getBucketKey).collect(Collectors.toSet());
        boolean coreRemoved = stored.stream().anyMatch(s -> s.isCore() && !submittedKeys.contains(s.getBucketKey()));
        if (coreRemoved) {
            errors.merge("buckets", CORE_REMOVED, (a, b) -> a + " " + b);
        }

        return new Submission(category, submitted, trusted, newOverride, Map.copyOf(errors));
    }

    /** Save a prepared, error-free submission; returns the settings id. */
    public long save(Submission submission) throws EntityNotFoundException {
        if (!submission.errors().isEmpty()) {
            throw new IllegalArgumentException("Refusing to save an allocation policy with errors");
        }
        AllocationSettings settings = submission.settings();
        long id = dao.saveSettingsAndBuckets(settings, submission.buckets())
                .orElseThrow(() -> new EntityNotFoundException("allocation settings",
                        settings.getId() == null ? 0 : settings.getId()));
        settings.setId(id);
        return id;
    }

    /** The name the preview's sentence uses: the category's label, else the newest profile's name. */
    public String sampleName(CategoryRef category) {
        String name = category != null ? category.label() : newestProfileName.get();
        return name == null ? "the recipient" : name;
    }

    private AllocationSettings defaultSettings() {
        return dao.findDefault()
                .orElseThrow(() -> new DataAccessException("Default allocation policy row is missing - re-run schema.sql"));
    }

    // the default's fee settings as an unsaved template
    private static AllocationSettings templateFrom(AllocationSettings base, long categoryId) {
        AllocationSettings template = new AllocationSettings();
        template.setCategoryId(categoryId);
        template.setFeeModel(base.getFeeModel());
        template.setFeePct(base.getFeePct());
        template.setFeeFixed(base.getFeeFixed());
        template.setAllowDonorAdjust(base.isAllowDonorAdjust());
        return template;
    }
}
