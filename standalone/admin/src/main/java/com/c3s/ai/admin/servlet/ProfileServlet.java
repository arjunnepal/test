package com.c3s.ai.admin.servlet;

import com.c3s.ai.admin.exception.EntityNotFoundException;
import com.c3s.ai.admin.model.Category;
import com.c3s.ai.admin.model.Profile;
import com.c3s.ai.admin.model.ProfileStatus;
import com.c3s.ai.admin.service.ProfileService;
import com.c3s.ai.admin.upload.UploadUtil;
import com.c3s.ai.admin.util.FlashMessages;
import com.c3s.ai.admin.util.Log;
import com.c3s.ai.admin.validation.ProfileValidator;
import com.c3s.ai.admin.web.Csrf;
import com.c3s.ai.admin.web.FlashScope;
import com.c3s.ai.admin.web.ProfileFormPageData;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// GET /profile [?id=X] - add/edit form. POST /profile - validate + save (insert if no id).
// Most of the mess in doPost is photo handling - see the notes there.
@WebServlet(name = "ProfileServlet", urlPatterns = "/profile")
@MultipartConfig(maxFileSize = ProfileServlet.MAX_PHOTO_BYTES,
        maxRequestSize = ProfileServlet.MAX_REQUEST_BYTES)
public class ProfileServlet extends AdminServlet {

    private static final Logger log = LoggerFactory.getLogger(ProfileServlet.class);
    private static final String FORM_VIEW = "/WEB-INF/views/profile-form.jsp";
    private static final String NOT_FOUND_MESSAGE = "That profile could not be found.";

    static final long MAX_PHOTO_MB = 5;
    static final long MAX_PHOTO_BYTES = MAX_PHOTO_MB * 1024 * 1024;
    static final long MAX_REQUEST_BYTES = (MAX_PHOTO_MB + 1) * 1024 * 1024;

    private final ProfileService service = new ProfileService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String idParam = req.getParameter("id");
        Profile profile;
        boolean isEdit;

        if (idParam == null || idParam.isBlank()) {
            profile = new Profile();
            isEdit = false;
        } else {
            Long id = parseId(idParam);
            if (id == null) {
                log.warn("Malformed profile id requested: {}", Log.clean(idParam));
                showNotFound(req, resp, NOT_FOUND_MESSAGE);
                return;
            }
            profile = loadOrRespond(req, resp, log, "profile id=" + id, NOT_FOUND_MESSAGE,
                    () -> service.get(id));
            if (profile == null) {
                return;
            }
            if (profile.getStatus() == ProfileStatus.ARCHIVED) {
                // block editing an archived profile even by a hand-typed URL
                log.info("Blocked edit attempt on archived profile id={}", id);
                FlashScope.push(req, FlashMessages.blockedEdit("profiles"));
                resp.sendRedirect(req.getContextPath() + "/profiles");
                return;
            }
            isEdit = true;
        }

        renderForm(req, resp, profile, isEdit, null, null, null);
    }

    private void renderForm(HttpServletRequest req, HttpServletResponse resp, Profile profile,
                            boolean isEdit, Map<String, String> errors, String pendingPhoto, String flash)
            throws ServletException, IOException {
        ProfileFormPageData data = new ProfileFormPageData();
        data.setEdit(isEdit);
        data.setProfile(profile);
        data.setSelectedCategoryIds(toIds(profile.getCategories()));
        data.setCategories(service.pickerCategories());
        data.setErrors(errors);
        data.setPendingPhoto(pendingPhoto);
        if (errors != null && !errors.isEmpty()) {
            renderRejected(req, resp, FORM_VIEW, data, flash);
        } else {
            render(req, resp, FORM_VIEW, data);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");

        // AuthFilter skips the CSRF check for this multipart route; do it here, now that
        // req.getParameter("_csrf") can see the form part.
        if (!Csrf.matches(req)) {
            log.warn("CSRF check failed for POST /profile");
            renderError(req, resp, HttpServletResponse.SC_FORBIDDEN,
                    "Your session could not be verified. Reload the page and try again.");
            return;
        }

        Map<String, String> errors = new LinkedHashMap<>();
        Profile profile = bindProfile(req, errors);

        String idParam = req.getParameter("id");
        boolean isEdit = idParam != null && !idParam.isBlank();
        Long editId = null;

        String existingPhotoFilename = null;
        if (isEdit) {
            editId = parseId(idParam);
            if (editId == null) {
                showNotFound(req, resp, NOT_FOUND_MESSAGE);
                return;
            }
            long lookupId = editId;
            Profile existing = loadOrRespond(req, resp, log, "profile id=" + lookupId, NOT_FOUND_MESSAGE,
                    () -> service.get(lookupId));
            if (existing == null) {
                return;
            }
            existingPhotoFilename = existing.getPhotoFilename();
            // don't let "Save as draft" quietly un-publish a live profile - that needs Archive
            if (existing.getStatus() == ProfileStatus.PUBLISHED && profile.getStatus() == ProfileStatus.DRAFT) {
                errors.put("status", "Published profile can't be saved as draft.");
            }
        }

        errors.putAll(mergeMissingOnly(errors, ProfileValidator.validate(profile)));

        Part photoPart;
        try {
            photoPart = req.getPart("photo");
        } catch (IllegalStateException e) {
            // getPart() throws this when the @MultipartConfig size limit is blown
            log.debug("Rejected oversized photo upload", e);
            photoPart = null;
            errors.put("photo", "Photo is too large (" + MAX_PHOTO_MB + "MB max).");
        }
        boolean hasNewFile = photoPart != null && photoPart.getSize() > 0;
        boolean removeRequested = req.getParameter("removePhotoFlag") != null;
        String newPhotoExt = null;
        if (hasNewFile) {
            newPhotoExt = UploadUtil.extractAllowedExtension(photoPart.getSubmittedFileName());
            if (newPhotoExt == null) {
                errors.put("photo", "Photo must be a JPG, PNG, or GIF image.");
            }
        }

        // a photo uploaded on an earlier failed submit, carried through the round-trip in a
        // hidden field (a file <input> can't be refilled). Trust it only if it matches our pattern.
        String priorPending = trim(req.getParameter("pendingPhoto"));
        if (priorPending != null && !UploadUtil.isSafeFilename(priorPending)) {
            priorPending = null;
        }

        String pendingPhotoFilename = null;
        if (hasNewFile && newPhotoExt != null) {
            try {
                pendingPhotoFilename = UploadUtil.save(photoPart, newPhotoExt);
                if (priorPending != null) {
                    UploadUtil.delete(priorPending);
                }
            } catch (UploadUtil.InvalidImageException e) {
                log.info("Rejected photo upload: {}", e.getMessage());
                errors.put("photo", e.getMessage());
                pendingPhotoFilename = priorPending;   // keep the previously-validated one, if any
            } catch (IOException e) {
                log.error("Failed to save uploaded photo for profile submission", e);
                showServerError(req, resp);
                return;
            }
        } else if (priorPending != null && !removeRequested) {
            pendingPhotoFilename = priorPending;
        } else if (priorPending != null) {
            // remove clicked on a pending upload that was never persisted - just drop the file
            UploadUtil.delete(priorPending);
        }

        if (!errors.isEmpty()) {
            log.info("Profile submission rejected ({} field errors): {}", errors.size(), errors.keySet());
            profile.setPhotoFilename(pendingPhotoFilename != null ? pendingPhotoFilename
                    : removeRequested ? null : existingPhotoFilename);
            // the un-publish error is specific enough to be the toast itself
            String flash = errors.containsKey("status") ? errors.get("status") : FlashMessages.validationFailed();
            renderForm(req, resp, profile, isEdit, errors, pendingPhotoFilename, flash);
            return;
        }

        String finalPhotoFilename = pendingPhotoFilename != null ? pendingPhotoFilename
                : removeRequested ? null : existingPhotoFilename;
        String photoToDelete = (pendingPhotoFilename != null || removeRequested) ? existingPhotoFilename : null;
        profile.setPhotoFilename(finalPhotoFilename);

        try {
            if (isEdit) {
                profile.setId(editId);
                service.update(profile);
                log.info("Updated profile id={} name={}", profile.getId(), Log.clean(profile.getName()));
            } else {
                long id = service.create(profile);
                profile.setId(id);
                log.info("Created profile id={} name={}", id, Log.clean(profile.getName()));
            }
        } catch (EntityNotFoundException e) {
            log.warn("Update submitted for missing profile id={}", Log.clean(idParam));
            showNotFound(req, resp, NOT_FOUND_MESSAGE);
            return;
        } catch (RuntimeException e) {
            log.error("Failed to save profile name={}", Log.clean(profile.getName()), e);
            showServerError(req, resp);
            return;
        }

        if (photoToDelete != null) {
            UploadUtil.delete(photoToDelete);
        }

        String flashMessage = profile.getStatus() == ProfileStatus.PUBLISHED
                ? FlashMessages.published(profile.getName())
                : FlashMessages.draftSaved(profile.getName());
        FlashScope.push(req, flashMessage);
        resp.sendRedirect(req.getContextPath() + "/profiles");
    }

    private Map<String, String> mergeMissingOnly(Map<String, String> existing, Map<String, String> toAdd) {
        Map<String, String> merged = new LinkedHashMap<>();
        toAdd.forEach((field, message) -> {
            if (!existing.containsKey(field)) {
                merged.put(field, message);
            }
        });
        return merged;
    }

    private Profile bindProfile(HttpServletRequest req, Map<String, String> errors) {
        Profile profile = new Profile();
        profile.setName(trim(req.getParameter("name")));
        profile.setCity(trim(req.getParameter("city")));
        profile.setState(trim(req.getParameter("state")));
        profile.setBlurb(trim(req.getParameter("blurb")));
        profile.setStory(trim(req.getParameter("story")));
        profile.setEmoji(trim(req.getParameter("emoji")));
        profile.setCategories(bindCategories(req));

        profile.setGoal(parseInt(req.getParameter("goal"), "goal", errors));
        Integer raised = parseInt(req.getParameter("raised"), "raised", errors);
        profile.setRaised(raised != null ? raised : 0);

        profile.setDeadline(parseDate(req.getParameter("deadline"), errors));

        profile.setIdentityVerified(req.getParameter("identityVerified") != null);
        profile.setNeedsReviewed(req.getParameter("needsReviewed") != null);
        profile.setPaypalConfirmed(req.getParameter("paypalConfirmed") != null);
        profile.setStoryReviewed(req.getParameter("storyReviewed") != null);

        // which submit button was clicked; anything but "published" is a draft
        profile.setStatus("published".equals(req.getParameter("status"))
                ? ProfileStatus.PUBLISHED : ProfileStatus.DRAFT);

        return profile;
    }

    // checkboxes only carry an id through to persistence / re-render
    private List<Category> bindCategories(HttpServletRequest req) {
        String[] raw = req.getParameterValues("categoryIds");
        List<Category> categories = new ArrayList<>();
        if (raw == null) {
            return categories;
        }
        for (String idStr : raw) {
            try {
                Category c = new Category();
                c.setId(Long.parseLong(idStr));
                categories.add(c);
            } catch (NumberFormatException e) {
                // garbage value, can't be a real checkbox - skip
                log.debug("Skipping malformed categoryIds value: {}", idStr);
            }
        }
        return categories;
    }

    private Set<Long> toIds(List<Category> categories) {
        Set<Long> ids = new HashSet<>();
        for (Category c : categories) {
            ids.add(c.getId());
        }
        return ids;
    }

    private Integer parseInt(String raw, String field, Map<String, String> errors) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            errors.put(field, "Must be a whole number.");
            return null;
        }
    }

    private LocalDate parseDate(String raw, Map<String, String> errors) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException e) {
            errors.put("deadline", "Must be a valid date.");
            return null;
        }
    }
}
