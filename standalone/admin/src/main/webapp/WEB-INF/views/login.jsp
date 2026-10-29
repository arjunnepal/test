<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.c3s.ai.admin.web.*, com.c3s.ai.admin.util.HtmlUtil" %>
<%
    LoginPageData data = (LoginPageData) request.getAttribute("LoginPageData");
    if (data == null) {
        data = new LoginPageData();
    }
    String ctx = request.getContextPath();
    String message = data.getMessage();
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <title>Sign in - C3S.ai Admin</title>
    <link href="https://fonts.googleapis.com/css2?family=Antonio:wght@400;500;700&amp;family=Palanquin:wght@300;400;500;600&amp;display=swap" rel="stylesheet"/>
    <link rel="stylesheet" href="<%= ctx %>/css/admin.css?v=10"/>
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
            <div class="lb-eyebrow">Admin Dashboard</div>
            <div class="lb-headline">Manage the <em>fund</em>.</div>
        </div>
    </div>

    <div class="login-form-side">
        <div class="login-card">
            <h1>C3S.ai Admin</h1>
            <div class="login-sub">Sign in to continue</div>

            <% if (message != null && !message.isEmpty()) { %>
                <div class="login-error"><%= HtmlUtil.esc(message) %></div>
            <% } %>

            <form method="post" action="<%= ctx %>/login">
                <input type="hidden" name="_csrf" value="<%= HtmlUtil.esc(Csrf.tokenFor(request)) %>"/>
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

            <div class="login-secure">
                <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"/></svg>
                Secure session
            </div>
        </div>
    </div>

</div>
</body>
</html>
