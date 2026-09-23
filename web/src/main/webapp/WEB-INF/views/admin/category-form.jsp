<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.c3s.ai.admin.category.*, com.c3s.ai.core.web.HtmlUtil, com.c3s.ai.core.web.Routes" %>
<%
    CategoryFormPageData data = (CategoryFormPageData) request.getAttribute("CategoryFormPageData");
    String message = data.getMessage();
    Category category = data.getCategory();
    boolean isEdit = data.isEdit();
    String noun = isEdit ? "Edit category" : "Add category";
    String pageTitle = noun + " - C3S.ai Admin";
    String crumb = noun;
    String activeSidebar = "categories";
    String activeTab = null;
    String[] extraScripts = {};
%>
<%@ include file="/WEB-INF/views/admin/common/header.jspf" %>

    <div class="form-header-row">
        <a class="tb-btn tb-btn-outline" href="<%= ctx %><%= Routes.ADMIN_CATEGORIES %>">&larr; Back to categories</a>
        <div class="fc-section-title">
            <%= isEdit ? "Edit category" : "New category" %>
        </div>
    </div>

    <form method="post" action="<%= ctx %><%= Routes.ADMIN_CATEGORY %>" id="category-form">
        <%= csrfInput %>
        <% if (isEdit) { %>
            <input type="hidden" name="id" value="<%= category.getId() %>"/>
        <% } %>

        <div class="form-card">
            <div class="fc-section">
                <div class="field">
                    <label for="c-label">Label <span class="required">*</span></label>
                    <input id="c-label" name="label" type="text" maxlength="100"
                           placeholder="e.g. Education" value="<%= HtmlUtil.esc(category.getLabel()) %>"/>
                    <%= HtmlUtil.fieldError(data, "label") %>
                </div>

                <div class="field field-narrow-sm">
                    <label for="c-emoji">Emoji</label>
                    <input id="c-emoji" name="emoji" type="text" maxlength="8"
                           value="<%= HtmlUtil.esc(category.getEmoji()) %>"/>
                    <%= HtmlUtil.fieldError(data, "emoji") %>
                </div>

                <div class="field field-narrow-md">
                    <label for="c-color">Color <span class="required">*</span></label>
                    <select id="c-color" name="colorClass">
                        <% for (CategoryFormPageData.ColorOption opt : data.getColorOptions()) { %>
                            <option value="<%= HtmlUtil.esc(opt.value()) %>"<%= opt.value().equals(category.getColorClass()) ? " selected" : "" %>><%= HtmlUtil.esc(opt.label()) %></option>
                        <% } %>
                    </select>
                    <%= HtmlUtil.fieldError(data, "colorClass") %>
                </div>
            </div>

            <div class="fc-section">
                <button type="submit" class="tb-btn tb-btn-primary tb-btn-block">
                    <%= isEdit ? "Save changes" : "Create category" %>
                </button>
            </div>
        </div>
    </form>

<%@ include file="/WEB-INF/views/admin/common/footer.jspf" %>
