<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.c3s.ai.admin.allocation.*, com.c3s.ai.admin.category.CategoryRef, com.c3s.ai.core.web.HtmlUtil, com.c3s.ai.core.web.Routes, java.util.List" %>
<%
    AllocationFormPageData data = (AllocationFormPageData) request.getAttribute("AllocationFormPageData");
    String message = data.getMessage();
    CategoryRef scopeCategory = data.getScopeCategory();
    Long categoryId = data.getCategoryId();
    AllocationSettings settings = data.getSettings();
    List<AllocationBucket> buckets = data.getBuckets();

    boolean isNew = data.isNewOverride();
    boolean isEdit = data.isEditOverride();
    String scopeLabel = scopeCategory != null ? scopeCategory.label() : null;

    String pageTitle = "Allocation settings - C3S.ai Admin";
    String crumb = "Allocation settings";
    String activeSidebar = "settings";
    String activeTab = "settings";
    String[] extraScripts = {"fee-split.js", "allocation-form.js"};

    int storedTotal = data.getBucketTotalPct();
    boolean balanced = storedTotal == 100;
    String totalNote = balanced ? " - balanced"
            : " - " + (storedTotal > 100 ? (storedTotal - 100) + "% over" : (100 - storedTotal) + "% unallocated");
%>
<%@ include file="/WEB-INF/views/admin/common/header.jspf" %>

    <div class="form-header-row">
        <a class="tb-btn tb-btn-outline" href="<%= ctx %><%= Routes.ADMIN_ALLOCATIONS %>">&larr; Back to allocation settings</a>
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
    <form method="post" action="<%= ctx %><%= Routes.ADMIN_ALLOCATION %>" id="alloc-form">
        <%= csrfInput %>
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
                        <%-- Every row the form can ever show is rendered here: the stored buckets, then
                             spares the Add button reveals. A spare row is hidden and its inputs are
                             disabled, so it submits nothing until then. --%>
                        <div id="bucket-rows">
                            <% for (int i = 0; i < AllocationFormPageData.MAX_BUCKETS; i++) {
                                AllocationBucket bucket = i < buckets.size() ? buckets.get(i) : null;
                                boolean spare = bucket == null;
                                String off = spare ? "disabled" : "";
                                boolean core = !spare && bucket.isCore();
                                boolean toRecipient = !spare && bucket.isToRecipient(); %>
                                <div class="bk-row" id="bucket-row-<%= i %>" data-bucket-idx="<%= i %>"<%= spare ? " hidden" : "" %>>
                                    <span class="bk-swatch <%= HtmlUtil.esc(data.getSwatchClass(i)) %>"></span>
                                    <div class="bk-fields">
                                        <input class="bk-name" type="text" name="bucketName_<%= i %>" placeholder="Bucket name"
                                               value="<%= spare ? "New allocation" : HtmlUtil.esc(bucket.getName()) %>"
                                               <%= core ? "readonly title=\"Core bucket - name is fixed\"" : "" %> <%= off %>/>
                                        <% if (toRecipient) { %>
                                            <div class="bk-purpose-static">Goes to the named recipient</div>
                                            <input type="hidden" name="bucketPurpose_<%= i %>" value=""/>
                                        <% } else { %>
                                            <input class="bk-purpose" type="text" name="bucketPurpose_<%= i %>"
                                                   value="<%= spare ? "" : HtmlUtil.esc(bucket.getPurpose()) %>"
                                                   placeholder="What this funds - blank uses a default description" <%= off %>/>
                                        <% } %>
                                        <%-- read by the live preview only; the server uses the stored description --%>
                                        <input type="hidden" name="bucketDesc_<%= i %>" value="<%= spare ? "" : HtmlUtil.esc(bucket.getDescription()) %>" <%= off %>/>
                                        <%-- identifies the stored bucket, whose core/recipient flags the server keeps --%>
                                        <input type="hidden" name="bucketKey_<%= i %>"
                                               value="<%= HtmlUtil.esc(spare ? data.getSpareBucketKey(i) : bucket.getBucketKey()) %>" <%= off %>/>
                                    </div>
                                    <div class="bk-pct">
                                        <input type="number" min="0" max="100" step="1" name="bucketPct_<%= i %>"
                                               value="<%= spare ? 0 : bucket.getPct() %>" class="bk-pct-input"
                                               data-recipient="<%= toRecipient %>" <%= off %>/><span>%</span>
                                    </div>
                                    <button type="button" class="bk-del" data-bucket-idx="<%= i %>" <%= core ? "disabled title=\"Core bucket - cannot be removed\"" : "title=\"Remove bucket\"" %>>&times;</button>
                                </div>
                            <% } %>
                        </div>

                        <%= HtmlUtil.fieldError(data, "buckets") %>
                        <%= HtmlUtil.fieldError(data, "bucketsTotal") %>
                        <%= HtmlUtil.fieldError(data, "bucketsName") %>
                        <%= HtmlUtil.fieldError(data, "bucketsRange") %>

                        <div class="bk-actions">
                            <button type="button" class="bk-add">+ Add allocation bucket</button>
                            <%-- filled in here as well as by the script, so it is right with JS off --%>
                            <span id="bk-total" class="bk-total <%= balanced ? "ok" : "bad" %>">Total <strong
                                    id="bk-total-pct"><%= storedTotal %>%</strong><span id="bk-total-note"><%= totalNote %></span>
                                <button type="button" class="bk-balance"<%= balanced ? " hidden" : "" %>>Balance</button></span>
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
                                <%= HtmlUtil.fieldError(data, "feePct") %>
                            </div>
                            <div class="field">
                                <label for="feeFixed">Fixed fee per transaction ($)</label>
                                <input id="feeFixed" name="feeFixed" type="number" min="0" max="5" step="0.01" value="<%= HtmlUtil.esc(settings.getFeeFixed()) %>"/>
                                <%= HtmlUtil.fieldError(data, "feeFixed") %>
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
                            <%= HtmlUtil.fieldError(data, "feeModel") %>
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

            <jsp:include page="/WEB-INF/views/admin/common/allocation-preview.jsp"/>
        </div>
    </form>


<%@ include file="/WEB-INF/views/admin/common/footer.jspf" %>
