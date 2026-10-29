package com.c3s.ai.admin.servlet;

import com.c3s.ai.admin.exception.EntityNotFoundException;
import com.c3s.ai.admin.model.AllocationBreakdown;
import com.c3s.ai.admin.model.AllocationBucket;
import com.c3s.ai.admin.model.AllocationCalculator;
import com.c3s.ai.admin.model.AllocationMessageBuilder;
import com.c3s.ai.admin.model.AllocationSettings;
import com.c3s.ai.admin.model.Category;
import com.c3s.ai.admin.model.FeeModel;
import com.c3s.ai.admin.service.AllocationService;
import com.c3s.ai.admin.service.ProfileService;
import com.c3s.ai.admin.util.FlashMessages;
import com.c3s.ai.admin.validation.AllocationValidator;
import com.c3s.ai.admin.web.AllocationFormPageData;
import com.c3s.ai.admin.web.FlashScope;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Add/edit form for an allocation policy - the default, or a per-category override.
 * A category with no override yet gets the default shown as an editable template.
 */
@WebServlet(name = "AllocationServlet", urlPatterns = "/allocation")
public class AllocationServlet extends AdminServlet {

    private static final Logger log = LoggerFactory.getLogger(AllocationServlet.class);
    private static final String VIEW = "/WEB-INF/views/allocation-settings.jsp";
    private static final String NOT_FOUND_MESSAGE = "That category could not be found.";
    private static final double PREVIEW_AMOUNT = 50.0;

    private final AllocationService service = new AllocationService();
    private final ProfileService profileService = new ProfileService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Long categoryId = parseId(req.getParameter("categoryId"));

        try {
            Category scopeCategory = null;
            if (categoryId != null) {
                scopeCategory = service.get(categoryId);
            }

            AllocationSettings defaultSettings = service.defaultSettings();
            AllocationSettings settings;
            boolean isNewOverride;
            long bucketsSettingsId;
            if (categoryId == null) {
                settings = defaultSettings;
                isNewOverride = false;
                bucketsSettingsId = defaultSettings.getId();
            } else {
                AllocationSettings override = service.override(categoryId);
                if (override != null) {
                    settings = override;
                    isNewOverride = false;
                    bucketsSettingsId = override.getId();
                } else {
                    settings = templateFromDefault(defaultSettings, categoryId);
                    isNewOverride = true;
                    bucketsSettingsId = defaultSettings.getId();
                }
            }

            List<AllocationBucket> buckets = service.buckets(bucketsSettingsId);

            renderForm(req, resp, categoryId, scopeCategory, settings, buckets, isNewOverride, Map.of());
        } catch (EntityNotFoundException e) {
            log.warn("Allocation settings requested for missing categoryId={}", categoryId);
            showNotFound(req, resp, NOT_FOUND_MESSAGE);
        } catch (RuntimeException e) {
            log.error("Failed to load allocation settings categoryId={}", categoryId, e);
            showServerError(req, resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        Long categoryId = parseId(req.getParameter("categoryId"));

        Map<String, String> errors = new LinkedHashMap<>();
        List<AllocationBucket> buckets = bindBuckets(req, errors);
        AllocationSettings settings = bindSettings(req, categoryId, errors);
        errors.putAll(AllocationValidator.validate(buckets, settings));

        Category scopeCategory = null;
        if (categoryId != null) {
            scopeCategory = loadOrRespond(req, resp, log, "category id=" + categoryId, NOT_FOUND_MESSAGE,
                    () -> service.get(categoryId));
            if (scopeCategory == null) {
                return;
            }
        }

        // target row is resolved server-side - the client never sends a settings id
        boolean isNewOverride;
        try {
            AllocationService.Target target = service.resolveTarget(categoryId);
            settings.setId(target.settingsId());
            isNewOverride = target.isNewOverride();
        } catch (RuntimeException e) {
            log.error("Failed to resolve allocation settings row categoryId={}", categoryId, e);
            showServerError(req, resp);
            return;
        }

        if (!errors.isEmpty()) {
            log.info("Allocation settings submission rejected ({} errors): {}", errors.size(), errors.keySet());
            renderForm(req, resp, categoryId, scopeCategory, settings, buckets, isNewOverride, errors);
            return;
        }

        try {
            long settingsId = service.save(settings, buckets);
            settings.setId(settingsId);
        } catch (EntityNotFoundException e) {
            log.warn("Allocation settings row disappeared during save categoryId={}", categoryId);
            showNotFound(req, resp, NOT_FOUND_MESSAGE);
            return;
        } catch (RuntimeException e) {
            log.error("Failed to save allocation settings categoryId={}", categoryId, e);
            showServerError(req, resp);
            return;
        }

        String flash = FlashMessages.policySaved(scopeCategory != null ? scopeCategory.getLabel() : null);
        FlashScope.push(req, flash);
        resp.sendRedirect(req.getContextPath() + "/allocations");
    }

    private void renderForm(HttpServletRequest req, HttpServletResponse resp, Long categoryId, Category scopeCategory,
                            AllocationSettings settings, List<AllocationBucket> buckets, boolean isNewOverride,
                            Map<String, String> errors)
            throws ServletException, IOException {
        AllocationBreakdown preview = AllocationCalculator.compute(PREVIEW_AMOUNT, settings, buckets);
        String sampleName = scopeCategory != null ? scopeCategory.getLabel()
                : profileService.newestActiveName();
        String donorMessage = AllocationMessageBuilder.build(sampleName, buckets, preview);

        // drives the heading and the "starting from the default" banner
        String action = categoryId == null ? "default" : (isNewOverride ? "new" : "edit");

        AllocationFormPageData data = new AllocationFormPageData();
        data.setAction(action);
        data.setCategoryId(categoryId);
        data.setScopeCategory(scopeCategory);
        data.setSettings(settings);
        data.setBuckets(buckets);
        data.setErrors(errors);
        data.setPreview(preview);
        data.setSampleName(sampleName == null ? "the recipient" : sampleName);
        data.setDonorMessage(donorMessage);
        if (errors.isEmpty()) {
            render(req, resp, VIEW, data);
        } else {
            renderRejected(req, resp, VIEW, data, FlashMessages.validationFailed());
        }
    }

    // the default's fee settings as an unsaved template
    private static AllocationSettings templateFromDefault(AllocationSettings base, long categoryId) {
        AllocationSettings template = new AllocationSettings();
        template.setCategoryId(categoryId);
        template.setFeeModel(base.getFeeModel());
        template.setFeePct(base.getFeePct());
        template.setFeeFixed(base.getFeeFixed());
        template.setAllowDonorAdjust(base.isAllowDonorAdjust());
        return template;
    }

    // buckets come in as bucketIndexes="0,1,2" plus bucketName_0 / bucketPct_0 / ... per index
    private List<AllocationBucket> bindBuckets(HttpServletRequest req, Map<String, String> errors) {
        List<AllocationBucket> buckets = new ArrayList<>();
        String indexes = req.getParameter("bucketIndexes");
        if (indexes == null || indexes.isBlank()) {
            return buckets;
        }
        int sortOrder = 0;
        for (String idx : indexes.split(",")) {
            idx = idx.trim();
            if (idx.isEmpty()) {
                continue;
            }
            AllocationBucket b = new AllocationBucket();
            // submitted order - drives the swatch color and is persisted as sort_order
            b.setSortOrder(sortOrder++);
            b.setBucketKey(trim(req.getParameter("bucketKey_" + idx)));
            b.setToRecipient(req.getParameter("bucketToRecipient_" + idx) != null);
            b.setCore(req.getParameter("bucketCore_" + idx) != null);
            b.setName(trim(req.getParameter("bucketName_" + idx)));
            b.setDescription(trim(req.getParameter("bucketDesc_" + idx)));
            b.setPurpose(trim(req.getParameter("bucketPurpose_" + idx)));

            String pctParam = req.getParameter("bucketPct_" + idx);
            try {
                b.setPct(pctParam == null || pctParam.isBlank() ? 0 : Integer.parseInt(pctParam.trim()));
            } catch (NumberFormatException e) {
                errors.put("bucketsRange", "Bucket shares must be whole numbers.");
            }
            buckets.add(b);
        }
        return buckets;
    }

    private AllocationSettings bindSettings(HttpServletRequest req, Long categoryId, Map<String, String> errors) {
        AllocationSettings settings = new AllocationSettings();
        settings.setCategoryId(categoryId);

        // a garbage feeModel is a field error, not a 500
        FeeModel feeModel = FeeModel.fromRequestValue(req.getParameter("feeModel"));
        if (feeModel == null) {
            errors.put("feeModel", "Choose who pays the processing fee.");
            feeModel = FeeModel.DEDUCTED;
        }
        settings.setFeeModel(feeModel);

        settings.setFeePct(parseDecimal(req.getParameter("feePct"), "feePct", "Processor rate", errors));
        settings.setFeeFixed(parseDecimal(req.getParameter("feeFixed"), "feeFixed", "Fixed fee", errors));
        settings.setAllowDonorAdjust(req.getParameter("allowDonorAdjust") != null);
        return settings;
    }

    private BigDecimal parseDecimal(String raw, String field, String label, Map<String, String> errors) {
        if (raw == null || raw.isBlank()) {
            errors.put(field, label + " is required.");
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            errors.put(field, label + " must be a number.");
            return BigDecimal.ZERO;
        }
    }
}
