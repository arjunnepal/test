<%@ page contentType="text/html;charset=UTF-8" isErrorPage="true" session="false" %>
<%@ page import="com.c3s.ai.browse.web.*, com.c3s.ai.browse.util.HtmlUtil" %>
<%
    // not set when Tomcat sends the error here itself
    ErrorPageData data = (ErrorPageData) request.getAttribute("ErrorPageData");
    String message = data != null && !data.getMessage().isBlank()
        ? data.getMessage()
        : "An unexpected error occurred. Please try again shortly.";
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <title>Something went wrong - C3S.ai &times; United Way NEFL</title>
    <link href="https://fonts.googleapis.com/css2?family=Antonio:wght@400;500;700&amp;family=Palanquin:wght@300;400;500;600&amp;display=swap" rel="stylesheet"/>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/browse.css?v=11"/>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/error.css?v=1"/>
</head>
<body>
<div class="err-wrap">
    <div class="err-card">
        <div class="err-icon">!</div>
        <h1>Something went wrong</h1>
        <p><%= HtmlUtil.esc(message) %></p>
        <a class="err-btn" href="<%= request.getContextPath() %>/">Back to giving</a>
    </div>
</div>
</body>
</html>
