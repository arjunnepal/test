<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.c3s.ai.admin.web.*, com.c3s.ai.admin.model.*, com.c3s.ai.admin.util.HtmlUtil, java.util.List" %>
<%
    AllocationFormPageData data = (AllocationFormPageData) request.getAttribute("AllocationFormPageData");
    AllocationBreakdown preview = data.getPreview();
    AllocationSettings settings = data.getSettings();
    String donorMessage = data.getDonorMessage();
    String sampleName = data.getSampleName();
    double previewFeePct = preview.charge() > 0 ? (preview.processing() / preview.charge() * 100) : 0;
    List<AllocationBreakdown.BucketAmount> previewBuckets = preview.buckets();
%>
<div>
    <div class="set-preview">
        <div class="set-preview-title">Live preview - $50 donation</div>
        <div class="alloc-bar" id="preview-bar">
            <%
                for (int st = 0; st < previewBuckets.size(); st++) {
                    AllocationBreakdown.BucketAmount b = previewBuckets.get(st);
                    if (st == 0) {
            %>
                    <div class="alloc-seg fee" data-pct="<%= previewFeePct %>" data-min-px="<%= previewFeePct > 0 ? 3 : 0 %>">
                        <% if (previewFeePct >= 13) { %><%= HtmlUtil.pct1(previewFeePct) %>%<% } %>
                    </div>
            <%
                    }
            %>
                <div class="alloc-seg <%= HtmlUtil.esc(b.colorClass()) %>" data-pct="<%= b.pctOfCharge() %>" data-min-px="<%= b.pctOfCharge() > 0 ? 3 : 0 %>">
                    <% if (b.pctOfCharge() >= 13) { %><%= HtmlUtil.pct1(b.pctOfCharge()) %>%<% } %>
                </div>
            <% } %>
        </div>
        <div class="alloc-rows">
            <div class="alloc-row">
                <span class="alloc-dot fee"></span>
                <span class="alloc-lbl">Processing fee<small id="preview-fee-label"><%= HtmlUtil.esc(settings.getFeePct()) %>% + <%= HtmlUtil.money(settings.getFeeFixed()) %></small></span>
                <span class="alloc-right"><span class="alloc-amt fee" id="preview-fee-amt"><%= HtmlUtil.money(preview.processing()) %></span></span>
            </div>
            <div id="preview-bucket-rows">
                <% for (AllocationBreakdown.BucketAmount b : previewBuckets) { %>
                    <div class="alloc-row">
                        <span class="alloc-dot <%= HtmlUtil.esc(b.colorClass()) %>"></span>
                        <span class="alloc-lbl"><%= HtmlUtil.esc(b.name()) %><small><%= b.pct() %>% of net gift</small></span>
                        <span class="alloc-right"><span class="alloc-amt <%= HtmlUtil.esc(b.colorClass()) %>"><%= HtmlUtil.money(b.amount()) %></span></span>
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
            <span id="preview-message"><%= donorMessage %></span>
        </div>
        <div class="set-note">Shown on the payment page and summarized on each profile. Recipient shown here is <strong id="preview-recipient-name"><%= HtmlUtil.esc(sampleName) %></strong>.</div>
    </div>
</div>

<%-- live-preview script (allocation-preview.js) is loaded by allocation-settings.jsp --%>
