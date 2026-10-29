<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.c3s.ai.admin.web.*, com.c3s.ai.admin.model.*, com.c3s.ai.admin.util.HtmlUtil, java.util.List" %>
<%
    AllocationFormPageData data = (AllocationFormPageData) request.getAttribute("AllocationFormPageData");
    String message = data.getMessage();
    String action = data.getAction();
    Category scopeCategory = data.getScopeCategory();
    Long categoryId = data.getCategoryId();
    AllocationSettings settings = data.getSettings();
    List<AllocationBucket> buckets = data.getBuckets();

    boolean isNew = "new".equals(action);
    boolean isEdit = "edit".equals(action);
    String scopeLabel = scopeCategory != null ? scopeCategory.getLabel() : null;

    String pageTitle = "Allocation settings - C3S.ai Admin";
    String crumb = "Allocation settings";
    String activeSidebar = "settings";
    String activeTab = "settings";
%>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

    <div class="form-header-row">
        <a class="tb-btn tb-btn-outline" href="<%= ctx %>/allocations">&larr; Back to allocation settings</a>
        <div class="fc-section-title">
            <% if (isNew) { %>New custom policy &ndash; <%= HtmlUtil.esc(scopeLabel) %><%
            } else if (isEdit) { %>Custom policy &ndash; <%= HtmlUtil.esc(scopeLabel) %><%
            } else { %>Default allocation policy<% } %>
        </div>
    </div>

    <% if (isNew) { %>
        <div class="set-note set-note-tight">
            This creates a new custom policy for <strong><%= HtmlUtil.esc(scopeLabel) %></strong>,
            starting from the current default policy shown below. Nothing is saved until you click
            <strong>Save allocation policy</strong>.
        </div>
    <% } %>
    <form method="post" action="<%= ctx %>/allocation" id="alloc-form">
        <input type="hidden" name="_csrf" value="<%= HtmlUtil.esc(Csrf.tokenFor(request)) %>"/>
        <% if (categoryId != null) { %><input type="hidden" name="categoryId" value="<%= categoryId %>"/><% } %>
        <input type="hidden" name="bucketIndexes" id="bucket-indexes" value="<%= HtmlUtil.esc(data.getBucketIndexesCsv()) %>"/>

        <div class="two-col-form">
            <div>
                <div class="form-card">
                    <div class="fc-section">
                        <div class="fc-section-title">Fund allocation policy</div>

                        <div class="bk-head">
                            <span></span><span>Destination account</span><span class="bk-head-pct">Share</span><span></span>
                        </div>
                        <div id="bucket-rows" data-next-index="<%= buckets.size() %>">
                            <% for (int st = 0; st < buckets.size(); st++) {
                                AllocationBucket bucket = buckets.get(st); %>
                                <div class="bk-row" id="bucket-row-<%= st %>">
                                    <span class="bk-swatch <%= HtmlUtil.esc(bucket.getColorClass()) %>"></span>
                                    <div class="bk-fields">
                                        <input class="bk-name" type="text" name="bucketName_<%= st %>" placeholder="Bucket name"
                                               value="<%= HtmlUtil.esc(bucket.getName()) %>" <%= bucket.isCore() ? "readonly title=\"Core bucket - name is fixed\"" : "" %>/>
                                        <% if (bucket.isToRecipient()) { %>
                                            <div class="bk-purpose-static">Goes to the named recipient</div>
                                            <input type="hidden" name="bucketPurpose_<%= st %>" value=""/>
                                        <% } else { %>
                                            <input class="bk-purpose" type="text" name="bucketPurpose_<%= st %>"
                                                   value="<%= HtmlUtil.esc(bucket.getPurpose()) %>"
                                                   placeholder="What this funds - blank uses a default description"/>
                                        <% } %>
                                        <input type="hidden" name="bucketDesc_<%= st %>" value="<%= HtmlUtil.esc(bucket.getDescription()) %>"/>
                                        <input type="hidden" name="bucketKey_<%= st %>" value="<%= HtmlUtil.esc(bucket.getBucketKey()) %>"/>
                                        <% if (bucket.isCore()) { %><input type="hidden" name="bucketCore_<%= st %>" value="1"/><% } %>
                                        <% if (bucket.isToRecipient()) { %><input type="hidden" name="bucketToRecipient_<%= st %>" value="1"/><% } %>
                                    </div>
                                    <div class="bk-pct">
                                        <input type="number" min="0" max="100" step="1" name="bucketPct_<%= st %>"
                                               value="<%= bucket.getPct() %>" class="bk-pct-input" data-recipient="<%= bucket.isToRecipient() %>"/><span>%</span>
                                    </div>
                                    <button type="button" class="bk-del" data-idx="<%= st %>" <%= bucket.isCore() ? "disabled title=\"Core bucket - cannot be removed\"" : "title=\"Remove bucket\"" %>>&times;</button>
                                </div>
                            <% } %>
                        </div>

                        <% if (data.getError("buckets") != null) { %><span class="field-error"><%= HtmlUtil.esc(data.getError("buckets")) %></span><% } %>
                        <% if (data.getError("bucketsTotal") != null) { %><span class="field-error"><%= HtmlUtil.esc(data.getError("bucketsTotal")) %></span><% } %>
                        <% if (data.getError("bucketsName") != null) { %><span class="field-error"><%= HtmlUtil.esc(data.getError("bucketsName")) %></span><% } %>
                        <% if (data.getError("bucketsRange") != null) { %><span class="field-error"><%= HtmlUtil.esc(data.getError("bucketsRange")) %></span><% } %>

                        <div class="bk-actions">
                            <button type="button" class="bk-add">+ Add allocation bucket</button>
                            <span id="bk-total" class="bk-total"></span>
                        </div>

                        <div class="set-note">
                            Shares apply to the <strong>net</strong> gift - after processing fees are settled - and must
                            total 100%. Restricted and Unrestricted are core buckets and cannot be removed; add as many
                            further destinations as you need (for example an administration or emergency-response fund).
                        </div>
                        <label class="set-lock">
                            <input type="checkbox" name="allowDonorAdjust" <%= settings.isAllowDonorAdjust() ? "checked" : "" %>/>
                            Let donors adjust the split themselves
                        </label>
                    </div>

                    <div class="fc-section">
                        <div class="fc-section-title">Processing fees</div>
                        <div class="set-fee-row">
                            <div class="field">
                                <label for="feePct">Processor rate (%)</label>
                                <input id="feePct" name="feePct" type="number" min="0" max="10" step="0.1" value="<%= HtmlUtil.esc(settings.getFeePct()) %>"/>
                                <% if (data.getError("feePct") != null) { %><span class="field-error"><%= HtmlUtil.esc(data.getError("feePct")) %></span><% } %>
                            </div>
                            <div class="field">
                                <label for="feeFixed">Fixed fee per transaction ($)</label>
                                <input id="feeFixed" name="feeFixed" type="number" min="0" max="5" step="0.01" value="<%= HtmlUtil.esc(settings.getFeeFixed()) %>"/>
                                <% if (data.getError("feeFixed") != null) { %><span class="field-error"><%= HtmlUtil.esc(data.getError("feeFixed")) %></span><% } %>
                            </div>
                        </div>
                        <div class="field field-fee-model">
                            <label>Who pays the processing fee</label>
                            <input type="hidden" name="feeModel" id="fee-model-input" value="<%= HtmlUtil.esc(settings.getFeeModel().dbValue()) %>"/>
                            <div class="fee-seg" id="fee-seg">
                                <button type="button" data-model="deducted">Deduct from gift</button>
                                <button type="button" data-model="added">Ask donor to cover</button>
                                <button type="button" data-model="absorbed">UWNEFL absorbs</button>
                            </div>
                            <% if (data.getError("feeModel") != null) { %><span class="field-error"><%= HtmlUtil.esc(data.getError("feeModel")) %></span><% } %>
                        </div>
                    </div>

                    <div class="fc-section">
                        <div class="fc-section-title">Donor message</div>
                        <div class="set-note set-note-flush">
                            The sentence donors see is generated from the buckets above. Give each non-recipient
                            bucket a short "what this funds" description in its row - leave it blank and it falls
                            back to that bucket's description, or its name if that's blank too.
                        </div>
                    </div>

                    <div class="fc-section">
                        <button type="submit" class="tb-btn tb-btn-primary tb-btn-block">Save allocation policy</button>
                    </div>
                </div>
            </div>

            <jsp:include page="/WEB-INF/views/common/allocation-preview.jsp"/>
        </div>
    </form>

<script src="<%= ctx %>/js/allocation-preview.js?v=1" defer="defer"></script>
<script src="<%= ctx %>/js/allocation-form.js?v=1" defer="defer"></script>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
