<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.c3s.ai.admin.web.*, com.c3s.ai.admin.model.Category, com.c3s.ai.admin.util.HtmlUtil" %>
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
%>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

    <div class="form-header-row">
        <a class="tb-btn tb-btn-outline" href="<%= ctx %>/categories">&larr; Back to categories</a>
        <div class="fc-section-title">
            <%= isEdit ? "Edit category" : "New category" %>
        </div>
    </div>

    <form method="post" action="<%= ctx %>/category" id="category-form">
        <input type="hidden" name="_csrf" value="<%= HtmlUtil.esc(Csrf.tokenFor(request)) %>"/>
        <% if (isEdit) { %>
            <input type="hidden" name="id" value="<%= category.getId() %>"/>
        <% } %>

        <div class="form-card">
            <div class="fc-section">
                <div class="field">
                    <label for="c-label">Label <span class="required">*</span></label>
                    <input id="c-label" name="label" type="text" maxlength="100"
                           placeholder="e.g. Education" value="<%= HtmlUtil.esc(category.getLabel()) %>"/>
                    <% if (data.getError("label") != null) { %><span class="field-error"><%= HtmlUtil.esc(data.getError("label")) %></span><% } %>
                </div>

                <div class="field field-narrow-sm">
                    <label for="c-emoji">Emoji</label>
                    <input id="c-emoji" name="emoji" type="text" maxlength="2"
                           value="<%= HtmlUtil.esc(category.getEmoji()) %>"/>
                </div>

                <div class="field field-narrow-md">
                    <label for="c-color">Color <span class="required">*</span></label>
                    <select id="c-color" name="colorClass">
                        <% for (CategoryFormPageData.ColorOption opt : data.getColorOptions()) { %>
                            <option value="<%= HtmlUtil.esc(opt.value()) %>"<%= opt.value().equals(category.getColorClass()) ? " selected" : "" %>><%= HtmlUtil.esc(opt.label()) %></option>
                        <% } %>
                    </select>
                    <% if (data.getError("colorClass") != null) { %><span class="field-error"><%= HtmlUtil.esc(data.getError("colorClass")) %></span><% } %>
                </div>
            </div>

            <div class="fc-section">
                <button type="submit" class="tb-btn tb-btn-primary tb-btn-block">
                    <%= isEdit ? "Save changes" : "Create category" %>
                </button>
            </div>
        </div>
    </form>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
