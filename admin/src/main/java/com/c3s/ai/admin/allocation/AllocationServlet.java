package com.c3s.ai.admin.allocation;

import com.c3s.ai.admin.category.CategoryRef;
import com.c3s.ai.admin.shell.AdminServlet;
import com.c3s.ai.admin.shell.FlashMessages;
import com.c3s.ai.common.exception.ConstraintViolationException;
import com.c3s.ai.common.exception.EntityNotFoundException;
import com.c3s.ai.common.util.Strings;
import com.c3s.ai.core.web.Routes;
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
 * GET /admin/allocation [?categoryId=X] - the default policy, or a per-category override; a
 * category with none yet gets the default as an editable template. POST validates and saves.
 */
@WebServlet(name = "AllocationServlet", urlPatterns = Routes.ADMIN_ALLOCATION)
public class AllocationServlet extends AdminServlet {

    private static final Logger log = LoggerFactory.getLogger(AllocationServlet.class);
    private static final String VIEW = "/WEB-INF/views/admin/allocation-settings.jsp";
    private static final String NOT_FOUND_MESSAGE = "That category could not be found.";
    private static final String CONCURRENT_SAVE =
            "Someone else saved this policy at the same time. Reload the page and try again.";
    private static final double PREVIEW_AMOUNT = 50.0;

    private final AllocationService service = new AllocationService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Long categoryId = parseId(req.getParameter("categoryId"));
        AllocationService.Form form = loadOrRespond(req, resp, log, "allocation policy categoryId=" + categoryId,
                NOT_FOUND_MESSAGE, () -> service.loadForm(categoryId));
        if (form != null) {
            renderForm(req, resp, form.category(), form.settings(), form.buckets(), form.newOverride(), Map.of(), null);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Long categoryId = parseId(req.getParameter("categoryId"));

        Map<String, String> bindingErrors = new LinkedHashMap<>();
        List<AllocationBucket> buckets = bindBuckets(req, bindingErrors);
        AllocationSettings settings = bindSettings(req, bindingErrors);

        AllocationService.Submission submission = loadOrRespond(req, resp, log,
                "allocation policy categoryId=" + categoryId, NOT_FOUND_MESSAGE,
                () -> service.prepare(categoryId, settings, buckets, bindingErrors));
        if (submission == null) {
            return;
        }
        if (!submission.errors().isEmpty()) {
            log.info("Allocation settings submission rejected ({} errors): {}",
                    submission.errors().size(), submission.errors().keySet());
            renderForm(req, resp, submission.category(), submission.settings(), submission.buckets(),
                    submission.newOverride(), submission.errors(), FlashMessages.validationFailed());
            return;
        }

        try {
            service.save(submission);
        } catch (ConstraintViolationException e) {
            // e.g. two admins creating the same category's first policy at once
            log.warn("Allocation settings save refused by a constraint categoryId={}: {}", categoryId, e.getMessage());
            renderForm(req, resp, submission.category(), submission.settings(), submission.buckets(),
                    submission.newOverride(), Map.of("buckets", CONCURRENT_SAVE), CONCURRENT_SAVE);
            return;
        } catch (EntityNotFoundException e) {
            log.warn("Allocation settings row disappeared during save categoryId={}", categoryId);
            showNotFound(req, resp, NOT_FOUND_MESSAGE);
            return;
        } catch (RuntimeException e) {
            log.error("Failed to save allocation settings categoryId={}", categoryId, e);
            showServerError(req, resp);
            return;
        }

        writesDone(req);
        CategoryRef category = submission.category();
        redirectWithFlash(req, resp, Routes.ADMIN_ALLOCATIONS,
                FlashMessages.policySaved(category != null ? category.label() : null));
    }

    private void renderForm(HttpServletRequest req, HttpServletResponse resp, CategoryRef category,
                            AllocationSettings settings, List<AllocationBucket> buckets, boolean newOverride,
                            Map<String, String> errors, String toast) throws ServletException, IOException {
        AllocationBreakdown preview = AllocationCalculator.compute(PREVIEW_AMOUNT, settings, buckets);
        String sampleName;
        try {
            sampleName = service.sampleName(category);
        } catch (RuntimeException e) {
            log.warn("Could not load the preview's sample name", e);
            sampleName = "the recipient";
        }
        AllocationFormPageData data = new AllocationFormPageData(category, settings, buckets, newOverride, preview,
                sampleName, AllocationMessageBuilder.build(sampleName, buckets, preview));
        data.setErrors(errors);
        if (data.hasErrors()) {
            showRejected(req, resp, VIEW, data, toast);
        } else {
            show(req, resp, VIEW, data);
        }
    }

    // bucketIndexes="0,1,2" plus bucketName_0 / bucketPct_0 / ... per index. Flags and
    // descriptions aren't read here - AllocationService takes those from the stored buckets.
    private static List<AllocationBucket> bindBuckets(HttpServletRequest req, Map<String, String> errors) {
        List<AllocationBucket> buckets = new ArrayList<>();
        String indexes = req.getParameter("bucketIndexes");
        if (Strings.isBlank(indexes)) {
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
            b.setBucketKey(Strings.trimToNull(req.getParameter("bucketKey_" + idx)));
            b.setName(trim(req.getParameter("bucketName_" + idx)));
            b.setPurpose(Strings.trimToNull(req.getParameter("bucketPurpose_" + idx)));

            String pctParam = req.getParameter("bucketPct_" + idx);
            try {
                b.setPct(Strings.isBlank(pctParam) ? 0 : Integer.parseInt(pctParam.trim()));
            } catch (NumberFormatException e) {
                errors.put("bucketsRange", "Bucket shares must be whole numbers.");
            }
            buckets.add(b);
        }
        return buckets;
    }

    private static AllocationSettings bindSettings(HttpServletRequest req, Map<String, String> errors) {
        AllocationSettings settings = new AllocationSettings();

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

    private static BigDecimal parseDecimal(String raw, String field, String label, Map<String, String> errors) {
        if (Strings.isBlank(raw)) {
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
