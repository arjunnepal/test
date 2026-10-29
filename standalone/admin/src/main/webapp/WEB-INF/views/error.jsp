<%@ page contentType="text/html;charset=UTF-8" isErrorPage="true" %>
<%@ page import="com.c3s.ai.admin.web.*, com.c3s.ai.admin.util.HtmlUtil" %>
<%
    // absent when a container-level 500 lands here with no servlet in between
    ErrorPageData data = (ErrorPageData) request.getAttribute("ErrorPageData");
    String message = data != null && !data.getMessage().isBlank()
        ? data.getMessage()
        : "An unexpected error occurred. Please try again shortly.";
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8"/>
    <title>Something went wrong - C3S.ai Admin</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/admin.css?v=10"/>
</head>
<body>
<div class="error-wrap">
    <div class="error-card">
        <div class="error-icon">!</div>
        <h1>Something went wrong</h1>
        <%-- never render the exception/stack trace here - it's logged server-side --%>
        <p><%= HtmlUtil.esc(message) %></p>
        <a class="btn-primary" href="<%= request.getContextPath() %>/profiles">Back to all profiles</a>
    </div>
</div>
</body>
</html>
