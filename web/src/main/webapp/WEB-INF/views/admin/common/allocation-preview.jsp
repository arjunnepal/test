<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.c3s.ai.admin.allocation.*, com.c3s.ai.core.web.HtmlUtil, java.util.List" %>
<%
    AllocationFormPageData data = (AllocationFormPageData) request.getAttribute("AllocationFormPageData");
    AllocationBreakdown preview = data.getPreview();
    AllocationSettings settings = data.getSettings();
    DonorMessage donorMessage = data.getDonorMessage();
    String sampleName = data.getSampleName();
    double previewFeePct = data.getPreviewFeePct();
    double minLabelled = AllocationFormPageData.MIN_LABELLED_SEGMENT_PCT;
    List<AllocationBreakdown.BucketAmount> previewBuckets = preview.buckets();
    int slots = AllocationFormPageData.MAX_BUCKETS;
%>
<%-- Every part of the preview is rendered here, once. Unused slots are hidden, and
     js/allocation-form.js shows and fills them as the policy is edited - it never builds markup. --%>
<div>
    <div class="set-preview">
        <div class="set-preview-title">Live preview - $50 donation</div>
        <div class="alloc-bar" id="preview-bar">
            <%-- a segment holds its label and nothing else, so it matches what the script writes --%>
            <div class="alloc-seg fee" id="preview-seg-fee" data-bar-pct="<%= previewFeePct %>"
                 data-bar-min-px="<%= previewFeePct > 0 ? 3 : 0 %>"<%= previewBuckets.isEmpty() ? " hidden" : "" %>><%--
             --%><% if (!previewBuckets.isEmpty() && previewFeePct >= minLabelled) { %><%= HtmlUtil.pct1(previewFeePct) %>%<% } %><%--
         --%></div>
            <% for (int i = 0; i < slots; i++) {
                AllocationBreakdown.BucketAmount b = i < previewBuckets.size() ? previewBuckets.get(i) : null; %>
                <div class="alloc-seg <%= HtmlUtil.esc(data.getSwatchClass(i)) %>" id="preview-seg-<%= i %>"
                     data-bar-pct="<%= b == null ? 0 : b.pctOfCharge() %>"
                     data-bar-min-px="<%= b != null && b.pctOfCharge() > 0 ? 3 : 0 %>"<%= b == null ? " hidden" : "" %>><%--
                 --%><% if (b != null && b.pctOfCharge() >= minLabelled) { %><%= HtmlUtil.pct1(b.pctOfCharge()) %>%<% } %><%--
             --%></div>
            <% } %>
        </div>
        <div class="alloc-rows">
            <div class="alloc-row">
                <span class="alloc-dot fee"></span>
                <span class="alloc-lbl">Processing fee<small id="preview-fee-label"><%= HtmlUtil.esc(settings.getFeePct()) %>% + <%= HtmlUtil.money(settings.getFeeFixed()) %></small></span>
                <span class="alloc-right"><span class="alloc-amt fee" id="preview-fee-amt"><%= HtmlUtil.money(preview.processing()) %></span></span>
            </div>
            <div id="preview-bucket-rows">
                <% for (int i = 0; i < slots; i++) {
                    AllocationBreakdown.BucketAmount b = i < previewBuckets.size() ? previewBuckets.get(i) : null;
                    String swatch = HtmlUtil.esc(data.getSwatchClass(i)); %>
                    <div class="alloc-row" id="preview-row-<%= i %>"<%= b == null ? " hidden" : "" %>>
                        <span class="alloc-dot <%= swatch %>"></span>
                        <span class="alloc-lbl"><span id="preview-row-name-<%= i %>"><%= b == null ? "" : HtmlUtil.esc(b.name()) %></span><small id="preview-row-share-<%= i %>"><%= b == null ? "" : b.pct() + "% of net gift" %></small></span>
                        <span class="alloc-right"><span class="alloc-amt <%= swatch %>" id="preview-row-amt-<%= i %>"><%= b == null ? "" : HtmlUtil.money(b.amount()) %></span></span>
                    </div>
                <% } %>
            </div>
        </div>
        <div class="alloc-total-row"><span>Donor is charged</span><strong id="preview-charge"><%= HtmlUtil.money(preview.charge()) %></strong></div>
    </div>

    <div class="set-preview">
        <div class="set-preview-title">Message donors will see</div>
        <div class="alloc-msg alloc-msg-flush">
            <span class="alloc-msg-icon">How your donation is used</span>
            <%-- One sentence, so the line breaks are swallowed by JSP comments rather than
                 becoming spaces in the middle of it. --%>
            <span id="preview-message"<%= donorMessage.isEmpty() ? " hidden" : "" %>><%--
             --%><span id="pm-fee"<%= donorMessage.hasFee() ? "" : " hidden" %>>After the <strong id="pm-fee-amt"><%= HtmlUtil.esc(donorMessage.feeAmount()) %></strong> processing fee, </span><%--
             --%><strong id="pm-recipient-pct"><%= donorMessage.recipientPct() %>%</strong> of <span id="pm-gift-ref"><%= HtmlUtil.esc(donorMessage.giftRef()) %></span> &mdash; <strong id="pm-recipient-amt"><%= HtmlUtil.esc(donorMessage.recipientAmount()) %></strong> &mdash; goes directly to support <span id="pm-who"><%= HtmlUtil.esc(donorMessage.who()) %></span><%--
             --%><% for (int i = 0; i < slots - 1; i++) {
                        DonorMessage.Part part = i < donorMessage.others().size() ? donorMessage.others().get(i) : null; %><%--
                 --%><span class="pm-other" id="pm-other-<%= i %>"<%= part == null ? " hidden" : "" %>><span><%= i == 0 ? ", and " : "; and " %></span><strong id="pm-other-pct-<%= i %>"><%= part == null ? "" : part.pct() + "%" %></strong> &mdash; <strong id="pm-other-amt-<%= i %>"><%= part == null ? "" : HtmlUtil.esc(part.amount()) %></strong> &mdash; supports <span id="pm-other-purpose-<%= i %>"><%= part == null ? "" : HtmlUtil.esc(part.purpose()) %></span></span><%--
             --%><% } %>.<%--
         --%></span>
        </div>
        <div class="set-note">Shown on the payment page and summarized on each profile. Recipient shown here is <strong id="preview-recipient-name"><%= HtmlUtil.esc(sampleName) %></strong>.</div>
    </div>
</div>
