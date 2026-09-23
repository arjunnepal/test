<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.c3s.ai.admin.allocation.AllocationListPageData, com.c3s.ai.admin.category.CategoryRef, com.c3s.ai.core.web.HtmlUtil, com.c3s.ai.core.web.Routes" %>
<%
    AllocationListPageData data = (AllocationListPageData) request.getAttribute("AllocationListPageData");
    String message = data.getMessage();
    String pageTitle = "Allocation settings - C3S.ai Admin";
    String crumb = "Allocation settings";
    String activeSidebar = "settings";
    String activeTab = "settings";
    String[] extraScripts = {};
%>
<%@ include file="/WEB-INF/views/admin/common/header.jspf" %>

    <div class="set-note set-note-tight">
        Every category uses the <strong>Default</strong> policy below unless it has its own custom policy
        saved. A category still on the default always reflects it live, so it never needs updating just
        because the default changed.
    </div>

    <div class="pt-table">
        <div class="pt-header pt-cols-3">
            <span>Category</span><span>Policy</span><span>Actions</span>
        </div>
        <div class="pt-row pt-cols-3">
            <div class="pt-person">
                <div class="pt-avatar cat-neutral">&#9881;&#65039;</div>
                <div class="pt-name">Default</div>
            </div>
            <span class="pt-tag pt-status pt-tag-disabled">Base policy</span>
            <div class="pt-actions">
                <a class="pt-act" href="<%= ctx %><%= Routes.ADMIN_ALLOCATION %>">Edit</a>
            </div>
        </div>
        <% for (CategoryRef cat : data.getCategories()) {
            boolean hasOverride = data.hasOverride(cat.id()); %>
            <div class="pt-row pt-cols-3">
                <div class="pt-person">
                    <div class="pt-avatar <%= HtmlUtil.esc(cat.colorClass()) %>"><%= HtmlUtil.esc(cat.emoji()) %></div>
                    <div class="pt-name"><%= HtmlUtil.esc(cat.label()) %></div>
                </div>
                <% if (hasOverride) { %>
                    <span class="pt-tag pt-status <%= HtmlUtil.esc(cat.colorClass()) %>">Custom policy</span>
                <% } else { %>
                    <span class="pt-tag pt-status pt-tag-disabled">Uses default</span>
                <% } %>
                <div class="pt-actions">
                    <% if (hasOverride) { %>
                        <a class="pt-act" href="<%= ctx %><%= Routes.ADMIN_ALLOCATION %>?categoryId=<%= cat.id() %>">Edit</a>
                    <% } else { %>
                        <a class="pt-act" href="<%= ctx %><%= Routes.ADMIN_ALLOCATION %>?categoryId=<%= cat.id() %>">+ Add</a>
                    <% } %>
                </div>
            </div>
        <% } %>
    </div>

<%@ include file="/WEB-INF/views/admin/common/footer.jspf" %>
