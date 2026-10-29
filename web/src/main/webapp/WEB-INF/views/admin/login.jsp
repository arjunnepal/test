<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.c3s.ai.core.web.HtmlUtil, com.c3s.ai.core.web.LoginPageData, com.c3s.ai.core.web.Routes" %>
<%
    LoginPageData data = (LoginPageData) request.getAttribute("LoginPageData");
    if (data == null) {
        data = new LoginPageData();
    }
    String ctx = request.getContextPath();
    String message = data.getMessage();
    // the staff sign-in for two doors, each opening its own session scope: the dashboard
    // (/admin/login) and the financial report (/report/login)
    boolean reportDoor = Routes.REPORT_LOGIN.equals(data.getAction());
    String action = reportDoor ? Routes.REPORT_LOGIN : Routes.ADMIN_LOGIN;
    // may create the session, so it has to run before any output
    String csrfInput = HtmlUtil.csrfInput(request);
    String pageTitle = reportDoor ? "Sign in - C3S.ai Financial Report" : "Sign in - C3S.ai Admin";
    String[] scripts = {};
    String stylesheet = "app.css";
%>
<!DOCTYPE html>
<html lang="en">
<head>
<%@ include file="/WEB-INF/views/common/head.jspf" %>
</head>
<body>

<div class="login-page">

    <div class="login-brand">
        <div class="lb-top">
            <div class="lb-uw-circle"><div class="lb-uw-inner"></div></div>
            <div class="lb-uw-name">United Way<small>NORTHEAST FLORIDA</small></div>
            <div class="lb-divider"></div>
            <span class="lb-c3s">C3S<span>.ai</span></span>
        </div>
        <div class="lb-mid">
            <% if (reportDoor) { %>
            <div class="lb-eyebrow">Financial Report</div>
            <div class="lb-headline">Follow every <em>dollar</em>.</div>
            <% } else { %>
            <div class="lb-eyebrow">Admin Dashboard</div>
            <div class="lb-headline">Manage the <em>fund</em>.</div>
            <% } %>
        </div>
    </div>

    <div class="login-form-side">
        <div class="login-card">
            <h1><%= reportDoor ? "C3S.ai Financial Report" : "C3S.ai Admin" %></h1>
            <div class="login-sub">Sign in to continue</div>

            <% if (message != null && !message.isEmpty()) { %>
                <div class="login-error"><%= HtmlUtil.esc(message) %></div>
            <% } %>

            <form method="post" action="<%= ctx %><%= action %>">
                <%= csrfInput %>
                <% if (data.getNext() != null) { %>
                <input type="hidden" name="next" value="<%= HtmlUtil.esc(data.getNext()) %>"/>
                <% } %>
                <div class="field">
                    <label for="username">Username</label>
                    <input id="username" name="username" type="text" autocomplete="username" autofocus
                           value="<%= HtmlUtil.esc(data.getUsername()) %>"/>
                </div>
                <div class="field">
                    <label for="password">Password</label>
                    <input id="password" name="password" type="password" autocomplete="current-password"/>
                </div>
                <button type="submit" class="tb-btn tb-btn-primary tb-btn-block">Sign in</button>
            </form>
            <%-- a way out for anyone who opened the wrong door --%>
            <a class="tb-btn tb-btn-danger tb-btn-block" href="<%= ctx %><%= Routes.BROWSE_HOME %>">Cancel</a>

            <%-- HTTPS only - claiming a secure session over plain HTTP would be misleading --%>
            <% if (request.isSecure()) { %>
            <div class="login-secure">
                <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"/></svg>
                Secure session
            </div>
            <% } %>
        </div>
    </div>

</div>
</body>
</html>
