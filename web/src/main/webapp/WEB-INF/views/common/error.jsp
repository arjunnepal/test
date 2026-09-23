<%@ page contentType="text/html;charset=UTF-8" isErrorPage="true" %>
<%@ page import="com.c3s.ai.core.web.PageData, com.c3s.ai.core.web.HtmlUtil, com.c3s.ai.core.web.Routes,
                 com.c3s.ai.core.auth.AuthSession, com.c3s.ai.core.auth.AuthSession.Scope" %>
<%
    // One error page for both halves of the app. Absent when a container-level error lands
    // here with no servlet in between.
    PageData data = (PageData) request.getAttribute("PageData");
    String ctx = request.getContextPath();

    // Style and wording follow the *admin session*, not the role: a donor who pokes at an
    // /admin URL has no admin scope, so they get the donor page, which is the point.
    boolean staff = AuthSession.isAuthenticated(request, Scope.ADMIN);

    // plain instanceof, not a pattern: Jasper compiles JSPs at its own (older) source level
    Object rawStatus = request.getAttribute("jakarta.servlet.error.status_code");
    int status = (rawStatus instanceof Integer) ? ((Integer) rawStatus).intValue() : 0;

    String message = data != null && !data.getMessage().isBlank() ? data.getMessage() : null;
    if (message == null) {
        // A "User" gets plain language and no status code; staff get something they can act on.
        if (staff) {
            message = status == 403
                ? "Your role doesn't have access to that page."
                : "An unexpected error occurred. Please try again shortly.";
        } else {
            message = status == 404
                ? "We couldn't find that page."
                : "Something went wrong on our end. Please try again shortly.";
        }
    }
    String pageTitle = "Something went wrong — C3S.ai" + (staff ? " Admin" : " × United Way NEFL");
    String[] scripts = {};
%>
<!DOCTYPE html>
<html lang="en">
<head>
<%@ include file="/WEB-INF/views/common/head.jspf" %>
</head>
<body>

<%-- never render the exception or stack trace here - it's logged server-side --%>
<%-- One card for both halves; only the heading, the wording and the link differ. --%>
<div class="err-wrap">
    <div class="err-card">
        <div class="err-icon">!</div>
        <h1>Something went wrong<%= staff && status > 0 ? " (" + status + ")" : "" %></h1>
        <p><%= HtmlUtil.esc(message) %></p>
<% if (staff) { %>
        <a class="err-btn" href="<%= ctx %><%= Routes.ADMIN_HOME %>">Back to all profiles</a>
<% } else { %>
        <a class="err-btn" href="<%= ctx %>/">Back to giving</a>
<% } %>
    </div>
</div>

</body>
</html>
