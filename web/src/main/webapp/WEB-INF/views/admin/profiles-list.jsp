<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.c3s.ai.admin.profile.*, com.c3s.ai.admin.category.CategoryRef, com.c3s.ai.core.web.HtmlUtil, com.c3s.ai.core.web.Routes" %>
<%
    ProfileListPageData data = (ProfileListPageData) request.getAttribute("ProfileListPageData");
    String message = data.getMessage();
    String pageTitle = "All profiles - C3S.ai Admin";
    String crumb = "All profiles";
    String activeSidebar = "list";
    String activeTab = "list";
    String[] extraScripts = {};
%>
<%@ include file="/WEB-INF/views/admin/common/header.jspf" %>

    <% if (data.getProfiles().isEmpty()) { %>
        <div class="pt-table">
            <div class="pt-empty">No profiles yet. <a href="<%= ctx %><%= Routes.ADMIN_PROFILE %>">Add the first one</a>.</div>
        </div>
    <% } else { %>
        <div class="pt-table" id="admin-profile-table">
            <div class="pt-header pt-cols-7">
                <span>Recipient</span><span>Category</span><span>Goal</span><span>Raised</span><span>Progress</span><span>Status</span><span>Actions</span>
            </div>
            <% for (ProfileListItem profile : data.getProfiles()) { %>
                <div class="pt-row pt-cols-7">
                    <div class="pt-person">
                        <% if (profile.hasPhoto()) { %>
                            <img class="pt-avatar pt-avatar-photo" src="<%= ctx %><%= Routes.PHOTOS_PREFIX %><%= HtmlUtil.esc(profile.photoFilename()) %>" alt=""/>
                        <% } else { %>
                            <div class="pt-avatar"><%= HtmlUtil.esc(profile.displayEmoji()) %></div>
                        <% } %>
                        <div>
                            <div class="pt-name"><%= HtmlUtil.esc(profile.name()) %></div>
                            <div class="pt-loc"><%= HtmlUtil.esc(profile.location()) %></div>
                        </div>
                    </div>
                    <div class="pt-tag-list">
                        <% for (CategoryRef cat : profile.categories()) { %>
                            <span class="pt-tag <%= HtmlUtil.esc(cat.colorClass()) %>"><%= HtmlUtil.esc(cat.label()) %></span>
                        <% } %>
                    </div>
                    <span>$<%= HtmlUtil.num(profile.goal()) %></span>
                    <span class="pt-raised">$<%= HtmlUtil.num(profile.raised()) %></span>
                    <div class="pt-pct-wrap">
                        <span class="pt-pct"><%= profile.percentRaised() %>%</span>
                        <div class="mini-prog"><div class="mini-prog-fill" data-bar-pct="<%= profile.percentRaised() %>"></div></div>
                    </div>
                    <span class="pt-tag pt-status <%= profile.status().badgeClass() %>"><%= profile.status().label() %></span>
                    <div class="pt-actions">
                        <% if (profile.isArchived()) { %>
                            <span class="pt-act pt-act-disabled" title="Archived profiles can't be edited">Edit</span>
                        <% } else { %>
                            <a class="pt-act" href="<%= ctx %><%= Routes.ADMIN_PROFILE %>?id=<%= profile.id() %>">Edit</a>
                            <form method="post" action="<%= ctx %><%= Routes.ADMIN_PROFILES_ARCHIVE %>"
                                  data-confirm-title="Archive <%= HtmlUtil.esc(profile.name()) %>?"
                                  data-confirm-body="It stays listed here as Archived, but drops off the donor-facing site and can no longer be edited."
                                  class="inline-form">
                                <%= csrfInput %>
                                <input type="hidden" name="id" value="<%= profile.id() %>"/>
                                <button type="button" class="pt-act danger" data-archive="true">Archive</button>
                            </form>
                        <% } %>
                    </div>
                </div>
            <% } %>
        </div>
    <% } %>

<%@ include file="/WEB-INF/views/admin/common/footer.jspf" %>
