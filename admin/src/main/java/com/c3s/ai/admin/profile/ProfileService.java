package com.c3s.ai.admin.profile;

import com.c3s.ai.admin.category.CategoryRef;
import com.c3s.ai.admin.category.CategoryService;
import com.c3s.ai.common.exception.ArchivedEntityException;
import com.c3s.ai.common.exception.EntityNotFoundException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Profile use cases and their rules, which the package-private DAO makes unskippable: archived
 * profiles stay read-only, a published profile can't become a draft, and categories must be active.
 */
public class ProfileService {

    static final String UNPUBLISH_REFUSED = "Published profile can't be saved as draft.";
    static final String CATEGORY_UNAVAILABLE = "One or more of the chosen categories is no longer available.";

    private final ProfileDao dao;
    private final Supplier<List<CategoryRef>> activeCategories;

    public ProfileService() {
        this(new JdbcProfileDao(), new CategoryService()::activeOptions);
    }

    ProfileService(ProfileDao dao, Supplier<List<CategoryRef>> activeCategories) {
        this.dao = dao;
        this.activeCategories = activeCategories;
    }

    public List<ProfileListItem> listAll() {
        return dao.findListItems();
    }

    /** The profile for its edit form. */
    public Profile getForEdit(long id) throws EntityNotFoundException, ArchivedEntityException {
        Profile profile = dao.findById(id).orElseThrow(() -> new EntityNotFoundException("profile", id));
        if (profile.getStatus() == ProfileStatus.ARCHIVED) {
            throw new ArchivedEntityException("profile", id);
        }
        return profile;
    }

    /** Status and photo of a profile about to be saved over. */
    public StoredState editableState(long id) throws EntityNotFoundException, ArchivedEntityException {
        StoredState state = dao.findStoredState(id).orElseThrow(() -> new EntityNotFoundException("profile", id));
        if (state.status() == ProfileStatus.ARCHIVED) {
            throw new ArchivedEntityException("profile", id);
        }
        return state;
    }

    public List<CategoryRef> pickerCategories() {
        return activeCategories.get();
    }

    /** Newest active profile's name for the allocation preview sample; null if none. */
    public String newestActiveName() {
        return dao.findNewestActiveName().orElse(null);
    }

    /**
     * Every rule for a submitted profile, and it swaps the submitted ids for full categories so a
     * re-rendered form shows their labels and colours. existing is null when creating.
     */
    public Map<String, String> validate(Profile submitted, StoredState existing) {
        Map<String, String> errors = new LinkedHashMap<>();

        Map<Long, CategoryRef> active = pickerCategories().stream()
                .collect(Collectors.toMap(CategoryRef::id, Function.identity()));
        List<CategoryRef> resolved = new ArrayList<>();
        for (CategoryRef ref : submitted.getCategories()) {
            CategoryRef full = active.get(ref.id());
            if (full == null) {
                errors.put("categories", CATEGORY_UNAVAILABLE);
            } else if (!resolved.contains(full)) {
                resolved.add(full);
            }
        }
        submitted.setCategories(resolved);

        ProfileValidator.validate(submitted, LocalDate.now()).forEach(errors::putIfAbsent);

        // unpublishing needs Archive, not "Save as draft"
        if (existing != null && existing.status() == ProfileStatus.PUBLISHED
                && submitted.getStatus() == ProfileStatus.DRAFT) {
            errors.put("status", UNPUBLISH_REFUSED);
        }
        return errors;
    }

    public long create(Profile profile) {
        long id = dao.insert(profile);
        profile.setId(id);
        return id;
    }

    public void update(Profile profile) throws EntityNotFoundException, ArchivedEntityException {
        long id = profile.getId();
        if (!dao.updateUnlessArchived(profile)) {
            // nothing editable matched: gone, or archived since the form was loaded
            dao.findStoredState(id).orElseThrow(() -> new EntityNotFoundException("profile", id));
            throw new ArchivedEntityException("profile", id);
        }
    }

    /** Soft-delete; returns the name for the confirmation message. */
    public String archive(long id) throws EntityNotFoundException {
        String name = dao.findName(id).orElseThrow(() -> new EntityNotFoundException("profile", id));
        if (!dao.archive(id)) {
            throw new EntityNotFoundException("profile", id);
        }
        return name;
    }

    /** Published count and total raised for the dashboard sidebar (SidebarStatsCache caches it). */
    public SidebarStats sidebarStats() {
        return dao.sidebarStats();
    }
}
