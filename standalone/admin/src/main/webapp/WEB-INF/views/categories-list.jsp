<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.c3s.ai.admin.web.*, com.c3s.ai.admin.model.*, com.c3s.ai.admin.util.HtmlUtil" %>
<%
    CategoryListPageData data = (CategoryListPageData) request.getAttribute("CategoryListPageData");
    String message = data.getMessage();
    String pageTitle = "Categories - C3S.ai Admin";
    String crumb = "Categories";
    String activeSidebar = "categories";
    String activeTab = null;
%>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

    <div class="list-toolbar">
        <a class="tb-btn tb-btn-primary list-toolbar-btn" href="<%= ctx %>/category">+ Add category</a>
    </div>

    <% if (data.getCategories() == null || data.getCategories().isEmpty()) { %>
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
                    <% if (cat.getStatus() == CategoryStatus.ACTIVE) { %>
                        <span class="pt-tag pt-status <%= HtmlUtil.esc(cat.getColorClass()) %>">Active</span>
                    <% } else { %>
                        <span class="pt-tag pt-status pt-tag-disabled">Archived</span>
                    <% } %>
                    <div class="pt-actions">
                        <% if (cat.getStatus() == CategoryStatus.ACTIVE) { %>
                            <a class="pt-act" href="<%= ctx %>/category?id=<%= cat.getId() %>">Edit</a>
                            <form method="post" action="<%= ctx %>/categories/archive"
                                  data-confirm-title="Archive <%= HtmlUtil.esc(cat.getLabel()) %>?"
                                  data-confirm-body="This removes it from the active list. Profiles that already reference it keep the association."
                                  class="inline-form">
                                <input type="hidden" name="_csrf" value="<%= HtmlUtil.esc(Csrf.tokenFor(request)) %>"/>
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

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
