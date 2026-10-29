<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.c3s.ai.admin.web.*, com.c3s.ai.admin.model.*, com.c3s.ai.admin.util.HtmlUtil" %>
<%
    ProfileListPageData data = (ProfileListPageData) request.getAttribute("ProfileListPageData");
    String message = data.getMessage();
    String pageTitle = "All profiles - C3S.ai Admin";
    String crumb = "All profiles";
    String activeSidebar = "list";
    String activeTab = "list";
%>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

    <% if (data.getProfiles() == null || data.getProfiles().isEmpty()) { %>
        <div class="pt-table">
            <div class="pt-empty">No profiles yet. <a href="<%= ctx %>/profile">Add the first one</a>.</div>
        </div>
    <% } else { %>
        <div class="pt-table" id="admin-profile-table">
            <div class="pt-header pt-cols-7">
                <span>Recipient</span><span>Category</span><span>Goal</span><span>Raised</span><span>Progress</span><span>Status</span><span>Actions</span>
            </div>
            <% for (Profile profile : data.getProfiles()) { %>
                <div class="pt-row pt-cols-7">
                    <div class="pt-person">
                        <% if (profile.hasPhoto()) { %>
                            <img class="pt-avatar pt-avatar-photo" src="<%= ctx %>/photos/<%= HtmlUtil.esc(profile.getPhotoFilename()) %>" alt=""/>
                        <% } else { %>
                            <div class="pt-avatar"><%= HtmlUtil.esc(profile.getDisplayEmoji()) %></div>
                        <% } %>
                        <div>
                            <div class="pt-name"><%= HtmlUtil.esc(profile.getName()) %></div>
                            <div class="pt-loc"><%= HtmlUtil.esc(profile.getCity()) %><%
                                if (profile.getCity() != null && !profile.getCity().isEmpty()
                                        && profile.getState() != null && !profile.getState().isEmpty()) { %>, <% }
                            %><%= HtmlUtil.esc(profile.getState()) %></div>
                        </div>
                    </div>
                    <div class="pt-tag-list">
                        <% for (Category cat : profile.getCategories()) { %>
                            <span class="pt-tag <%= HtmlUtil.esc(cat.getColorClass()) %>"><%= HtmlUtil.esc(cat.getLabel()) %></span>
                        <% } %>
                    </div>
                    <span>$<%= HtmlUtil.esc(profile.getGoal()) %></span>
                    <span class="pt-raised">$<%= HtmlUtil.esc(profile.getRaised()) %></span>
                    <div class="pt-pct-wrap">
                        <span class="pt-pct"><%= profile.getPercentRaised() %>%</span>
                        <div class="mini-prog"><div class="mini-prog-fill" data-pct="<%= profile.getPercentRaised() %>"></div></div>
                    </div>
                    <% if (profile.getStatus() == ProfileStatus.PUBLISHED) { %>
                        <span class="pt-tag pt-status status-published">Published</span>
                    <% } else if (profile.getStatus() == ProfileStatus.DRAFT) { %>
                        <span class="pt-tag pt-status status-draft">Draft</span>
                    <% } else { %>
                        <span class="pt-tag pt-status pt-tag-disabled">Archived</span>
                    <% } %>
                    <div class="pt-actions">
                        <% if (profile.getStatus() == ProfileStatus.ARCHIVED) { %>
                            <span class="pt-act pt-act-disabled" title="Archived profiles can't be edited">Edit</span>
                        <% } else { %>
                            <a class="pt-act" href="<%= ctx %>/profile?id=<%= profile.getId() %>">Edit</a>
                            <form method="post" action="<%= ctx %>/profiles/archive"
                                  data-confirm-title="Archive <%= HtmlUtil.esc(profile.getName()) %>?"
                                  data-confirm-body="It stays listed here as Archived, but drops off the donor-facing site and can no longer be edited."
                                  class="inline-form">
                                <input type="hidden" name="_csrf" value="<%= HtmlUtil.esc(Csrf.tokenFor(request)) %>"/>
                                <input type="hidden" name="id" value="<%= profile.getId() %>"/>
                                <button type="button" class="pt-act danger" data-archive="true">Archive</button>
                            </form>
                        <% } %>
                    </div>
                </div>
            <% } %>
        </div>
    <% } %>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
