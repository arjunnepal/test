<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.c3s.ai.admin.web.*, com.c3s.ai.admin.model.*, com.c3s.ai.admin.util.HtmlUtil" %>
<%
    ProfileFormPageData data = (ProfileFormPageData) request.getAttribute("ProfileFormPageData");
    String message = data.getMessage();
    Profile profile = data.getProfile();
    boolean isEdit = data.isEdit();
    String noun = isEdit ? "Edit profile" : "Add new profile";
    String pageTitle = noun + " - C3S.ai Admin";
    String crumb = noun;
    String activeSidebar = "add";
    String activeTab = "add";
%>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

    <form method="post" action="<%= ctx %>/profile" id="profile-form" enctype="multipart/form-data" novalidate="novalidate">
        <%-- multipart, so AuthFilter can't check this before the parts parse; ProfileServlet does --%>
        <input type="hidden" name="_csrf" value="<%= HtmlUtil.esc(Csrf.tokenFor(request)) %>"/>
        <% if (isEdit) { %>
            <input type="hidden" name="id" value="<%= profile.getId() %>"/>
        <% } %>

        <div class="two-col-form">
            <div>
                <div class="form-card">

                    <div class="fc-section">
                        <div class="fc-section-title">Photo / avatar</div>
                        <div class="photo-upload">
                            <input type="file" id="photo-input" name="photo" accept="image/*" class="hidden"/>
                            <div id="photo-placeholder" class="<%= profile.hasPhoto() ? "hidden" : "" %>">
                                <div class="photo-icon">&#128248;</div>
                                <div class="photo-label">Upload a photo</div>
                                <div class="photo-sub">Or we'll use a category emoji</div>
                            </div>
                            <div class="photo-preview<%= profile.hasPhoto() ? "" : " hidden" %>" id="photo-preview">
                                <img id="preview-img" src="<%= profile.hasPhoto() ? ctx + "/photos/" + HtmlUtil.esc(profile.getPhotoFilename()) : "" %>" alt=""/>
                                <button type="button" class="photo-remove">&#10005;</button>
                            </div>
                        </div>
                        <%-- profile-form.js enables this when the user removes an existing photo --%>
                        <input type="hidden" id="remove-photo-input" name="removePhotoFlag" value="true" disabled="disabled"/>
                        <%-- carries an uploaded-but-unsaved photo through a validation re-render
                             (the file input can't be refilled from the server) --%>
                        <% if (data.getPendingPhoto() != null && !data.getPendingPhoto().isEmpty()) { %>
                            <input type="hidden" name="pendingPhoto" value="<%= HtmlUtil.esc(data.getPendingPhoto()) %>"/>
                        <% } %>
                        <% if (data.getError("photo") != null) { %><span class="field-error"><%= HtmlUtil.esc(data.getError("photo")) %></span><% } %>
                    </div>

                    <div class="fc-section">
                        <div class="fc-section-title">Basic info</div>

                        <div class="field">
                            <label for="f-name">Name or alias <span class="required">*</span></label>
                            <input id="f-name" name="name" type="text" maxlength="100"
                                   placeholder="e.g. Marcus T." value="<%= HtmlUtil.esc(profile.getName()) %>"/>
                            <% if (data.getError("name") != null) { %><span class="field-error"><%= HtmlUtil.esc(data.getError("name")) %></span><% } %>
                        </div>

                        <div class="field-row">
                            <div class="field">
                                <label for="f-city">City</label>
                                <input id="f-city" name="city" type="text" placeholder="Jacksonville" value="<%= HtmlUtil.esc(profile.getCity()) %>"/>
                            </div>
                            <div class="field">
                                <label for="f-state">State</label>
                                <input id="f-state" name="state" type="text" maxlength="2" placeholder="FL" value="<%= HtmlUtil.esc(profile.getState()) %>"/>
                                <% if (data.getError("state") != null) { %><span class="field-error"><%= HtmlUtil.esc(data.getError("state")) %></span><% } %>
                            </div>
                        </div>

                        <div class="field">
                            <label for="f-blurb">Short blurb <span class="required">*</span></label>
                            <textarea id="f-blurb" name="blurb" rows="2" maxlength="160"
                                      placeholder="One or two sentences..."
                                      data-count-target="blurb-count"><%= HtmlUtil.esc(profile.getBlurb()) %></textarea>
                            <div class="char-count"><span id="blurb-count"><%= profile.getBlurb() == null ? 0 : profile.getBlurb().length() %></span>/160</div>
                            <% if (data.getError("blurb") != null) { %><span class="field-error"><%= HtmlUtil.esc(data.getError("blurb")) %></span><% } %>
                        </div>

                        <div class="field">
                            <label for="emoji-input">Emoji (optional)</label>
                            <input id="emoji-input" name="emoji" type="text" maxlength="2" class="input-emoji"
                                   placeholder="🏠" value="<%= HtmlUtil.esc(profile.getOwnEmoji()) %>"/>
                        </div>
                    </div>

                    <div class="fc-section">
                        <div class="fc-section-title">Categories <span class="required">*</span></div>
                        <div class="cat-pills" id="cat-pills">
                            <% for (Category cat : data.getCategories()) { %>
                                <label class="cat-pill <%= HtmlUtil.esc(cat.getColorClass()) %>">
                                    <input type="checkbox" name="categoryIds" value="<%= cat.getId() %>"
                                           data-label="<%= HtmlUtil.esc(cat.getLabel()) %>" data-emoji="<%= HtmlUtil.esc(cat.getEmoji()) %>" data-color="<%= HtmlUtil.esc(cat.getColorClass()) %>"
                                           <%= data.isSelected(cat.getId()) ? "checked" : "" %>/>
                                    <%= HtmlUtil.esc(cat.getEmoji()) %>&nbsp;<%= HtmlUtil.esc(cat.getLabel()) %>
                                </label>
                            <% } %>
                        </div>
                        <% if (data.getError("categories") != null) { %><span class="field-error"><%= HtmlUtil.esc(data.getError("categories")) %></span><% } %>
                    </div>

                    <div class="fc-section">
                        <div class="fc-section-title">Full story</div>
                        <div class="field">
                            <textarea id="f-story" name="story" rows="5" maxlength="2000"
                                      placeholder="2-4 paragraphs about their situation and goals..."
                                      data-count-target="story-count"><%= HtmlUtil.esc(profile.getStory()) %></textarea>
                            <div class="char-count"><span id="story-count"><%= profile.getStory() == null ? 0 : profile.getStory().length() %></span>/2000</div>
                            <% if (data.getError("story") != null) { %><span class="field-error"><%= HtmlUtil.esc(data.getError("story")) %></span><% } %>
                        </div>
                    </div>

                    <div class="fc-section">
                        <div class="fc-section-title">Funding goal</div>
                        <div class="field-row">
                            <div class="field">
                                <label for="f-goal">Goal ($) <span class="required">*</span></label>
                                <input id="f-goal" name="goal" type="number" min="100" step="50"
                                       placeholder="3500" value="<%= HtmlUtil.esc(profile.getGoal()) %>"/>
                                <% if (data.getError("goal") != null) { %><span class="field-error"><%= HtmlUtil.esc(data.getError("goal")) %></span><% } %>
                            </div>
                            <div class="field">
                                <label for="f-raised">Already raised ($)</label>
                                <input id="f-raised" name="raised" type="number" min="0" step="10"
                                       placeholder="0" value="<%= HtmlUtil.esc(profile.getRaised()) %>"/>
                                <% if (data.getError("raised") != null) { %><span class="field-error"><%= HtmlUtil.esc(data.getError("raised")) %></span><% } %>
                            </div>
                        </div>
                        <div class="field">
                            <label for="f-deadline">Deadline</label>
                            <div class="deadline-opts">
                                <button type="button" class="dl-opt<%= profile.getDeadline() == null ? " active" : "" %>" data-val="30d">30 days</button>
                                <button type="button" class="dl-opt" data-val="60d">60 days</button>
                                <button type="button" class="dl-opt" data-val="90d">90 days</button>
                                <button type="button" class="dl-opt<%= profile.getDeadline() != null ? " active" : "" %>" data-val="custom">Custom</button>
                            </div>
                            <input id="f-deadline" name="deadline" type="date" value="<%= profile.getDeadline() == null ? "" : profile.getDeadline() %>"
                                   class="deadline-field<%= profile.getDeadline() == null ? " hidden" : "" %>"/>
                            <% if (data.getError("deadline") != null) { %><span class="field-error"><%= HtmlUtil.esc(data.getError("deadline")) %></span><% } %>
                        </div>
                    </div>

                    <div class="fc-section">
                        <div class="fc-section-title">Verification</div>
                        <div class="verify-checks">
                            <label class="vc-item"><input type="checkbox" name="identityVerified" <%= profile.isIdentityVerified() ? "checked" : "" %>/>&nbsp;Identity verified by UWNEFL admin</label>
                            <label class="vc-item"><input type="checkbox" name="needsReviewed" <%= profile.isNeedsReviewed() ? "checked" : "" %>/>&nbsp;Need documentation reviewed</label>
                            <label class="vc-item"><input type="checkbox" name="paypalConfirmed" <%= profile.isPaypalConfirmed() ? "checked" : "" %>/>&nbsp;PayPal recipient email confirmed</label>
                            <label class="vc-item"><input type="checkbox" name="storyReviewed" <%= profile.isStoryReviewed() ? "checked" : "" %>/>&nbsp;Story reviewed for accuracy</label>
                        </div>
                    </div>
                </div>
            </div>

            <div>
                <div class="pn-title">Live preview</div>
                <div class="preview-card">
                    <div class="pc-img <%= profile.getFirstCategory() != null ? HtmlUtil.esc(profile.getFirstCategory().getColorClass()) : "" %>" id="pc-img"<% if (profile.hasPhoto()) { %> data-photo-url="<%= ctx %>/photos/<%= HtmlUtil.esc(profile.getPhotoFilename()) %>"<% } %>>
                        <%
                            String ownEmoji = profile.getOwnEmoji();
                            String pcEmoji;
                            if (ownEmoji != null && !ownEmoji.isEmpty()) {
                                pcEmoji = ownEmoji;
                            } else if (profile.getFirstCategory() != null) {
                                pcEmoji = profile.getFirstCategory().getEmoji();
                            } else {
                                pcEmoji = "🏠";
                            }
                        %>
                        <span id="pc-emoji" class="<%= profile.hasPhoto() ? "hidden" : "" %>"><%= HtmlUtil.esc(pcEmoji) %></span>
                    </div>
                    <div class="pc-body">
                        <div class="pc-name" id="pc-name"><%= profile.getName() != null && !profile.getName().isEmpty() ? HtmlUtil.esc(profile.getName()) : "Recipient name" %></div>
                        <div class="pc-tag-list" id="pc-tag-list">
                            <% for (Category cat : profile.getCategories()) { %>
                                <span class="pc-tag <%= HtmlUtil.esc(cat.getColorClass()) %>"><%= HtmlUtil.esc(cat.getLabel()) %></span>
                            <% } %>
                        </div>
                        <div class="pc-blurb" id="pc-blurb"><%= HtmlUtil.esc(profile.getBlurb()) %></div>
                        <div class="pc-prog-row"><span class="pc-prog-label" id="pc-goal-lbl">$<%= profile.getRaised() != null ? profile.getRaised() : 0 %> of $<%= profile.getGoal() != null ? profile.getGoal() : 0 %></span><span class="pc-pct" id="pc-pct"><%= profile.getPercentRaised() %>%</span></div>
                        <div class="pc-prog-track"><div class="pc-fill" id="pc-fill" data-pct="<%= profile.getPercentRaised() %>"></div></div>
                        <button type="button" class="pc-btn" data-goto="page-profile">View profile &amp; donate</button>
                    </div>
                </div>
                <div class="pub-actions">
                    <button type="submit" name="status" value="published" class="pub-btn">✓ Publish to donor site</button>
                    <button type="submit" name="status" value="draft" class="save-btn">Save as draft</button>
                </div>
                <div class="preview-note">Server-validated on save - required fields are marked with *.</div>
            </div>
        </div>
    </form>

<script src="<%= ctx %>/js/profile-form.js?v=1" defer="defer"></script>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
