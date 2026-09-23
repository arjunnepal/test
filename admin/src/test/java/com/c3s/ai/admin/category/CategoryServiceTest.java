package com.c3s.ai.admin.category;

import com.c3s.ai.common.exception.ArchivedEntityException;
import com.c3s.ai.common.exception.ConstraintViolationException;
import com.c3s.ai.common.exception.EntityNotFoundException;
import org.junit.jupiter.api.Test;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategoryServiceTest {

    /** In-memory stand-in with the real DAO's contract, including "no update on an archived row". */
    private static final class FakeDao implements CategoryDao {
        final List<Category> rows = new ArrayList<>();
        RuntimeException nextWriteFailure;

        Category add(long id, String label, CategoryStatus status) {
            Category c = new Category();
            c.setId(id);
            c.setLabel(label);
            c.setStatus(status);
            rows.add(c);
            return c;
        }

        public List<Category> findAll() {
            return rows;
        }

        public List<CategoryRef> findActiveRefs() {
            return rows.stream().filter(Category::isActive)
                    .map(c -> new CategoryRef(c.getId(), c.getLabel(), c.getEmoji(), c.getColorClass())).toList();
        }

        public Optional<Category> findById(long id) {
            return rows.stream().filter(c -> c.getId() == id).findFirst();
        }

        public Optional<String> findLabel(long id) {
            return findById(id).map(Category::getLabel);
        }

        public boolean labelExists(String label, Long excludeId) {
            return rows.stream().anyMatch(c -> c.getLabel().equalsIgnoreCase(label) && !c.getId().equals(excludeId));
        }

        public long insert(Category category) {
            failIfAsked();
            return 99;
        }

        public boolean updateActive(Category category) {
            failIfAsked();
            return findById(category.getId()).filter(Category::isActive).isPresent();
        }

        public boolean archive(long id) {
            return findById(id).map(c -> {
                c.setStatus(CategoryStatus.ARCHIVED);
                return true;
            }).orElse(false);
        }

        private void failIfAsked() {
            if (nextWriteFailure != null) {
                RuntimeException e = nextWriteFailure;
                nextWriteFailure = null;
                throw e;
            }
        }
    }

    private final FakeDao dao = new FakeDao();
    private final CategoryService service = new CategoryService(dao);

    private static Category submitted(Long id, String label) {
        Category c = new Category();
        c.setId(id);
        c.setLabel(label);
        return c;
    }

    @Test
    void anArchivedCategoryCannotBeOpenedForEditing() {
        dao.add(1, "Education", CategoryStatus.ARCHIVED);
        assertThrows(ArchivedEntityException.class, () -> service.getForEdit(1));
        assertThrows(EntityNotFoundException.class, () -> service.getForEdit(2));
    }

    // the old servlet only checked on GET; a hand-made POST could still edit an archived row
    @Test
    void anArchivedCategoryCannotBeUpdatedEvenByADirectSave() {
        dao.add(1, "Education", CategoryStatus.ARCHIVED);
        assertThrows(ArchivedEntityException.class, () -> service.update(submitted(1L, "Renamed")));
    }

    @Test
    void updatingAMissingCategoryIsNotFound() {
        assertThrows(EntityNotFoundException.class, () -> service.update(submitted(7L, "Anything")));
    }

    @Test
    void validationCoversLabelEmojiColourAndUniqueness() {
        dao.add(1, "Education", CategoryStatus.ACTIVE);

        Category bad = submitted(null, "education");
        bad.setEmoji("x".repeat(9));
        bad.setColorClass("cat-chartreuse");
        Map<String, String> errors = service.validate(bad);

        assertEquals(CategoryService.DUPLICATE_LABEL, errors.get("label"));
        assertTrue(errors.containsKey("emoji"));
        assertTrue(errors.containsKey("colorClass"));
        assertTrue(service.validate(submitted(1L, "Education")).isEmpty(), "a category doesn't clash with itself");
    }

    // two admins saving the same label at once: the unique key catches it, and it's a form error
    @Test
    void aDuplicateLabelRaceBecomesAFieldErrorNotAServerError() throws Exception {
        dao.nextWriteFailure = new ConstraintViolationException("Insert category violated a database constraint",
                new SQLIntegrityConstraintViolationException("Duplicate entry 'Arts' for key 'uwnefl_categories.label'"));
        assertEquals(Map.of("label", CategoryService.DUPLICATE_LABEL), service.create(submitted(null, "Arts")));
    }

    @Test
    void otherConstraintViolationsAreNotHidden() {
        dao.nextWriteFailure = new ConstraintViolationException("x",
                new SQLIntegrityConstraintViolationException("Check constraint 'uwnefl_chk_categories_color' is violated"));
        assertThrows(ConstraintViolationException.class, () -> service.create(submitted(null, "Arts")));
    }

    @Test
    void archiveReturnsTheLabel() throws Exception {
        dao.add(3, "Housing", CategoryStatus.ACTIVE);
        assertEquals("Housing", service.archive(3));
        assertThrows(EntityNotFoundException.class, () -> service.archive(4));
    }

    @Test
    void getActiveRefusesArchivedCategories() {
        dao.add(5, "Veterans", CategoryStatus.ARCHIVED);
        assertThrows(EntityNotFoundException.class, () -> service.getActive(5));
    }
}
