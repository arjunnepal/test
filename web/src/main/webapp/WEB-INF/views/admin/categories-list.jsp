<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.c3s.ai.admin.category.*, com.c3s.ai.core.web.HtmlUtil, com.c3s.ai.core.web.Routes" %>
<%
    CategoryListPageData data = (CategoryListPageData) request.getAttribute("CategoryListPageData");
    String message = data.getMessage();
    String pageTitle = "Categories - C3S.ai Admin";
    String crumb = "Categories";
    String activeSidebar = "categories";
    String activeTab = null;
    String[] extraScripts = {};
%>
<%@ include file="/WEB-INF/views/admin/common/header.jspf" %>

    <div class="list-toolbar">
        <a class="tb-btn tb-btn-primary list-toolbar-btn" href="<%= ctx %><%= Routes.ADMIN_CATEGORY %>">+ Add category</a>
    </div>

    <% if (data.getCategories().isEmpty()) { %>
        <div class="pt-table"><div class="pt-empty">No categories yet.</div></div>
    <% } else { %>
        <div class="pt-table">
            <div class="pt-header pt-cols-3">
                <span>Category</span><span>Status</span><span>Actions</span>
            </div>
            <% for (Category cat : data.getCategories()) { %>
                <div class="pt-row pt-cols-3">
                    <div class="pt-person">
                        <div class="pt-avatar <%= HtmlUtil.esc(cat.getColorClass()) %>"><%= HtmlUtil.esc(cat.getEmoji()) %></div>
                        <div class="pt-name"><%= HtmlUtil.esc(cat.getLabel()) %></div>
                    </div>
                    <span class="pt-tag pt-status <%= cat.isActive() ? HtmlUtil.esc(cat.getColorClass()) : "pt-tag-disabled" %>"><%= cat.getStatus().label() %></span>
                    <div class="pt-actions">
                        <% if (cat.isActive()) { %>
                            <a class="pt-act" href="<%= ctx %><%= Routes.ADMIN_CATEGORY %>?id=<%= cat.getId() %>">Edit</a>
                            <form method="post" action="<%= ctx %><%= Routes.ADMIN_CATEGORIES_ARCHIVE %>"
                                  data-confirm-title="Archive <%= HtmlUtil.esc(cat.getLabel()) %>?"
                                  data-confirm-body="This removes it from the active list. Profiles that already reference it keep the association."
                                  class="inline-form">
                                <%= csrfInput %>
                                <input type="hidden" name="id" value="<%= cat.getId() %>"/>
                                <button type="button" class="pt-act danger" data-archive="true">Archive</button>
                            </form>
                        <% } else { %>
                            <span class="pt-act pt-act-disabled" title="Archived categories can't be edited">Edit</span>
                        <% } %>
                    </div>
                </div>
            <% } %>
        </div>
    <% } %>

<%@ include file="/WEB-INF/views/admin/common/footer.jspf" %>
