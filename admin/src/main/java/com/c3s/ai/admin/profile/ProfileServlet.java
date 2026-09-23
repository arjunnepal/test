package com.c3s.ai.admin.profile;

import com.c3s.ai.admin.category.CategoryRef;
import com.c3s.ai.admin.shell.AdminServlet;
import com.c3s.ai.admin.shell.FlashMessages;
import com.c3s.ai.admin.shell.PendingUploads;
import com.c3s.ai.admin.shell.UploadUtil;
import com.c3s.ai.common.exception.ArchivedEntityException;
import com.c3s.ai.common.exception.EntityNotFoundException;
import com.c3s.ai.common.util.Log;
import com.c3s.ai.common.util.Strings;
import com.c3s.ai.core.auth.Csrf;
import com.c3s.ai.core.servlet.ErrorPages;
import com.c3s.ai.core.web.Routes;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GET /admin/profile [?id=X] - add/edit form. POST /admin/profile - validate + save (insert if no id).
 * The rules live in ProfileService; most of what's left here is photo handling.
 */
@WebServlet(name = "ProfileServlet", urlPatterns = Routes.ADMIN_PROFILE)
@MultipartConfig(maxFileSize = ProfileServlet.MAX_PHOTO_BYTES, maxRequestSize = ProfileServlet.MAX_REQUEST_BYTES)
public class ProfileServlet extends AdminServlet {

    static final String NOT_FOUND_MESSAGE = "That profile could not be found.";

    private static final Logger log = LoggerFactory.getLogger(ProfileServlet.class);
    private static final String VIEW = "/WEB-INF/views/admin/profile-form.jsp";

    static final long MAX_PHOTO_MB = 5;
    static final long MAX_PHOTO_BYTES = MAX_PHOTO_MB * 1024 * 1024;
    static final long MAX_REQUEST_BYTES = (MAX_PHOTO_MB + 1) * 1024 * 1024;

    private final ProfileService service = new ProfileService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String idParam = req.getParameter("id");
        if (Strings.isBlank(idParam)) {
            renderForm(req, resp, new Profile(), false, null, null, null);
            return;
        }
        Long id = parseId(idParam);
        if (id == null) {
            log.warn("Malformed profile id requested: {}", Log.clean(idParam));
            showNotFound(req, resp, NOT_FOUND_MESSAGE);
            return;
        }
        Profile profile = loadEditableOrRespond(req, resp, log, "profile id=" + id, NOT_FOUND_MESSAGE,
                Routes.ADMIN_PROFILES, "profiles", () -> service.getForEdit(id));
        if (profile != null) {
            renderForm(req, resp, profile, true, null, null, null);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // AuthFilter skips the CSRF check for this multipart route; do it here, now that
        // req.getParameter("_csrf") can see the form part.
        if (!Csrf.matches(req)) {
            log.warn("CSRF check failed for POST {}", Routes.ADMIN_PROFILE);
            ErrorPages.render(req, resp, HttpServletResponse.SC_FORBIDDEN,
                    "Your session could not be verified. Reload the page and try again.");
            return;
        }

        Map<String, String> errors = new LinkedHashMap<>();
        Profile profile = bind(req, errors);

        String idParam = req.getParameter("id");
        boolean isEdit = !Strings.isBlank(idParam);
        StoredState existing = null;
        if (isEdit) {
            Long id = parseId(idParam);
            if (id == null) {
                showNotFound(req, resp, NOT_FOUND_MESSAGE);
                return;
            }
            // archived profiles are read-only, however this POST was made
            existing = loadEditableOrRespond(req, resp, log, "profile id=" + id, NOT_FOUND_MESSAGE,
                    Routes.ADMIN_PROFILES, "profiles", () -> service.editableState(id));
            if (existing == null) {
                return;
            }
            profile.setId(id);
        }
        String existingPhoto = existing != null ? existing.photoFilename() : null;

        service.validate(profile, existing).forEach(errors::putIfAbsent);

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

        // A photo uploaded on an earlier failed submit, carried in a hidden field (a file <input>
        // can't be refilled). Only honoured if this session uploaded it.
        String priorPending = Strings.trimToNull(req.getParameter("pendingPhoto"));
        if (priorPending != null && !PendingUploads.contains(req, priorPending)) {
            log.warn("Ignoring a pendingPhoto this session didn't upload: {}", Log.clean(priorPending));
            priorPending = null;
        }

        String pendingPhoto = null;
        if (hasNewFile && newPhotoExt != null) {
            try {
                pendingPhoto = UploadUtil.save(photoPart, newPhotoExt);
                PendingUploads.add(req, pendingPhoto);
                discardPending(req, priorPending);
            } catch (UploadUtil.InvalidImageException e) {
                log.info("Rejected photo upload: {}", e.getMessage());
                errors.put("photo", e.getMessage());
                pendingPhoto = priorPending;   // keep the previously-validated one, if any
            } catch (IOException e) {
                log.error("Failed to save uploaded photo for profile submission", e);
                showServerError(req, resp);
                return;
            }
        } else if (priorPending != null && !removeRequested) {
            pendingPhoto = priorPending;
        } else if (priorPending != null) {
            // remove clicked on a pending upload that was never saved - just drop the file
            discardPending(req, priorPending);
        }

        String finalPhoto = pendingPhoto != null ? pendingPhoto : removeRequested ? null : existingPhoto;
        profile.setPhotoFilename(finalPhoto);

        if (!errors.isEmpty()) {
            log.info("Profile submission rejected ({} field errors): {}", errors.size(), errors.keySet());
            // the un-publish error is specific enough to be the toast itself
            String toast = errors.containsKey("status") ? errors.get("status") : FlashMessages.validationFailed();
            renderForm(req, resp, profile, isEdit, errors, pendingPhoto, toast);
            return;
        }

        try {
            if (isEdit) {
                service.update(profile);
            } else {
                service.create(profile);
            }
        } catch (ArchivedEntityException e) {
            log.info("Profile id={} was archived before the save", profile.getId());
            discardPending(req, pendingPhoto);
            redirectWithFlash(req, resp, Routes.ADMIN_PROFILES, FlashMessages.blockedEdit("profiles"));
            return;
        } catch (EntityNotFoundException e) {
            log.warn("Update submitted for missing profile id={}", profile.getId());
            discardPending(req, pendingPhoto);
            showNotFound(req, resp, NOT_FOUND_MESSAGE);
            return;
        } catch (RuntimeException e) {
            log.error("Failed to save profile name={}", Log.clean(profile.getName()), e);
            discardPending(req, pendingPhoto);
            showServerError(req, resp);
            return;
        }
        log.info("{} profile id={} name={}", isEdit ? "Updated" : "Created", profile.getId(), Log.clean(profile.getName()));

        // the new photo now belongs to the profile; the one it replaced (or that was removed) goes
        PendingUploads.remove(req, pendingPhoto);
        if (existingPhoto != null && !existingPhoto.equals(finalPhoto)) {
            UploadUtil.delete(existingPhoto);
        }

        writesDone(req);
        redirectWithFlash(req, resp, Routes.ADMIN_PROFILES, profile.getStatus() == ProfileStatus.PUBLISHED
                ? FlashMessages.published(profile.getName())
                : FlashMessages.draftSaved(profile.getName()));
    }

    private void renderForm(HttpServletRequest req, HttpServletResponse resp, Profile profile, boolean isEdit,
                            Map<String, String> errors, String pendingPhoto, String toast)
            throws ServletException, IOException {
        List<CategoryRef> picker = loadListOrRespond(req, resp, log, "category picker", service::pickerCategories);
        if (picker == null) {
            return;
        }
        ProfileFormPageData data = new ProfileFormPageData(profile, picker, pendingPhoto);
        data.setEdit(isEdit);
        data.setErrors(errors);
        if (data.hasErrors()) {
            showRejected(req, resp, VIEW, data, toast);
        } else {
            show(req, resp, VIEW, data);
        }
    }

    private static void discardPending(HttpServletRequest req, String filename) {
        if (filename != null) {
            PendingUploads.remove(req, filename);
            UploadUtil.delete(filename);
        }
    }

    private static Profile bind(HttpServletRequest req, Map<String, String> errors) {
        Profile profile = new Profile();
        profile.setName(trim(req.getParameter("name")));
        profile.setCity(Strings.trimToNull(req.getParameter("city")));
        profile.setState(Strings.trimToNull(req.getParameter("state")));
        profile.setBlurb(trim(req.getParameter("blurb")));
        profile.setStory(Strings.trimToNull(req.getParameter("story")));
        profile.setEmoji(Strings.trimToNull(req.getParameter("emoji")));
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
        profile.setStatus("published".equals(req.getParameter("status")) ? ProfileStatus.PUBLISHED : ProfileStatus.DRAFT);
        return profile;
    }

    // checkboxes only carry an id; ProfileService checks each against the active categories
    private static List<CategoryRef> bindCategories(HttpServletRequest req) {
        String[] raw = req.getParameterValues("categoryIds");
        List<CategoryRef> categories = new ArrayList<>();
        if (raw == null) {
            return categories;
        }
        for (String value : raw) {
            Long id = parseId(value);
            if (id != null) {
                categories.add(CategoryRef.idOnly(id));
            } else {
                log.debug("Skipping malformed categoryIds value: {}", Log.clean(value));
            }
        }
        return categories;
    }

    private static Integer parseInt(String raw, String field, Map<String, String> errors) {
        if (Strings.isBlank(raw)) {
            return null;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            errors.put(field, "Must be a whole number.");
            return null;
        }
    }

    private static LocalDate parseDate(String raw, Map<String, String> errors) {
        if (Strings.isBlank(raw)) {
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
