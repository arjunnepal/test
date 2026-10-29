<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.c3s.ai.browse.web.*, com.c3s.ai.browse.util.HtmlUtil" %>
<%
    LoginPageData data = (LoginPageData) request.getAttribute("LoginPageData");
    if (data == null) {
        data = new LoginPageData();
    }
    String ctx = request.getContextPath();
    String message = data.getMessage();
    String username = data.getUsername();
    boolean focusPassword = username != null && !username.isBlank();
    // may create the session, so it has to run before any output
    String csrfToken = Csrf.tokenFor(request);
%>
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1.0"/>
<title>Sign in &mdash; C3S.ai &times; United Way NEFL</title>
<link href="https://fonts.googleapis.com/css2?family=Antonio:wght@400;500;700&amp;family=Palanquin:wght@300;400;500;600&amp;display=swap" rel="stylesheet"/>
<link rel="stylesheet" href="<%= ctx %>/css/browse.css?v=11"/>
<link rel="stylesheet" href="<%= ctx %>/css/login.css?v=2"/>
<script src="<%= ctx %>/js/login.js?v=1" defer="defer"></script>
</head>
<body>

<div class="auth-page">

  <aside class="auth-brand">
    <a class="ab-logo" href="<%= ctx %>/">
      <span class="ab-circle"><span class="ab-circle-inner"></span></span>
      <span class="ab-name">United Way<small>NORTHEAST FLORIDA</small></span>
      <span class="ab-divider"></span>
      <span class="ab-c3s">C3S<span>.ai</span></span>
    </a>

    <div class="ab-mid">
      <div class="ab-eyebrow"><span class="ey-dot"></span>Giving Platform</div>
      <p class="ab-headline">Give directly to <em>real neighbors</em>.</p>
      <p class="ab-lede">Browse verified profiles and support people across Northeast Florida.</p>
      <ul class="ab-points">
        <li><span class="ab-check"><svg fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 13l4 4L19 7"/></svg></span>100% of every donation goes to the recipient</li>
        <li><span class="ab-check"><svg fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 13l4 4L19 7"/></svg></span>United Way NEFL covers all platform fees</li>
        <li><span class="ab-check"><svg fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 13l4 4L19 7"/></svg></span>Payments secured by PayPal</li>
      </ul>
    </div>

    <div class="ab-foot">United Way Northeast Florida &middot; C3S.ai Giving Platform</div>
  </aside>

  <main class="auth-form-side">
    <a class="auth-back" href="<%= ctx %>/">
      <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 17l-5-5m0 0l5-5m-5 5h12"/></svg>
      Back to giving
    </a>

    <div class="auth-card">
      <h1>Welcome <em>back</em></h1>
      <p class="auth-sub">Sign in to your giving account</p>

      <% if (!message.isEmpty()) { %>
        <div class="auth-error" role="alert">
          <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
          <span><%= HtmlUtil.esc(message) %></span>
        </div>
      <% } %>

      <form method="post" action="<%= ctx %>/login">
        <input type="hidden" name="<%= Csrf.PARAM %>" value="<%= HtmlUtil.esc(csrfToken) %>"/>

        <div class="auth-field">
          <label for="username">Username</label>
          <div class="auth-input">
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"/></svg>
            <input id="username" name="username" type="text" autocomplete="username" required
                   placeholder="Your username" value="<%= HtmlUtil.esc(username) %>"<% if (!focusPassword) { %> autofocus<% } %>/>
          </div>
        </div>

        <div class="auth-field">
          <label for="password">Password</label>
          <div class="auth-input">
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"/></svg>
            <input id="password" name="password" type="password" autocomplete="current-password" required
                   class="has-reveal" placeholder="Your password"<% if (focusPassword) { %> autofocus<% } %>/>
            <%-- shown by login.js --%>
            <button class="auth-reveal" type="button" data-reveal="password" aria-pressed="false" aria-label="Show password" hidden>Show</button>
          </div>
        </div>

        <button class="auth-submit" type="submit">
          Sign in
          <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7l5 5m0 0l-5 5m5-5H6"/></svg>
        </button>
        <a class="auth-cancel" href="<%= ctx %>/">Cancel</a>
      </form>

      <%-- HTTPS only --%>
      <% if (request.isSecure()) { %>
      <div class="auth-secure">
        <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"/></svg>
        Secure session
      </div>
      <% } %>
    </div>

    <p class="auth-alt">Admin? <a href="/admin">Sign in to the admin dashboard</a></p>
  </main>

</div>
</body>
</html>
