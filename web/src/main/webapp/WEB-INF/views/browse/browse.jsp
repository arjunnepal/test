<%-- session="false" so anonymous visitors don't get a session --%>
<%@ page contentType="text/html;charset=UTF-8" session="false" %>
<%@ page import="com.c3s.ai.browse.page.*, com.c3s.ai.browse.donation.TickerItem, com.c3s.ai.core.web.HtmlUtil, com.c3s.ai.core.web.Routes, com.c3s.ai.core.auth.AuthSession, com.c3s.ai.core.auth.AuthSession.Scope, java.util.List" %>
<%
    BrowsePageData data = (BrowsePageData) request.getAttribute("BrowsePageData");
    String ctx = request.getContextPath();

    List<ProfileCard> cards = data.getProfiles();
    List<CategoryChip> chips = data.getChips();
    List<CategoryTotal> catTotals = data.getCategoryTotals();
    List<TickerItem> ticker = data.getTicker();
    BrowseStats stats = data.getStats();
    int shown = cards.size();

    String signedInUser = AuthSession.currentUser(request, Scope.USER);
    String signedInName = AuthSession.currentDisplayName(request, Scope.USER);
    // only a signed-in donor has a form to protect; anonymous visitors get no session
    String csrfInput = signedInUser != null ? HtmlUtil.csrfInput(request) : null;

    String pageTitle = "C3S.ai × United Way NEFL — Give";
    String[] scripts = {"core.js", "card-filter.js"};
    boolean brandSticky = false;
    String brandLabel = "GIVING PLATFORM · NORTHEAST FLORIDA";
%>
<!DOCTYPE html>
<html lang="en">
<head>
<%@ include file="/WEB-INF/views/common/head.jspf" %>
</head>
<body>

<div class="page active">

<%@ include file="/WEB-INF/views/common/brand.jspf" %>

  <% if (!ticker.isEmpty()) { %>
  <div class="ticker-bar">
    <div class="ticker-label"><span class="ticker-dot"></span>This Week's Donors</div>
    <div class="ticker-track">
      <div class="ticker-inner">
        <%-- rendered twice so the scroll loops without a gap --%>
        <% for (int pass = 0; pass < 2; pass++) {
             for (TickerItem t : ticker) { %>
          <span class="ticker-item"<% if (pass == 1) { %> aria-hidden="true"<% } %>><span class="ti-amt">$<%= HtmlUtil.num(t.amount()) %></span> <span style="font-weight:400"><%= HtmlUtil.esc(t.donorLabel()) %></span> <span class="ti-sep">&rarr;</span> <span style="color:var(--mid)"><%= HtmlUtil.esc(t.recipient()) %></span> <span class="ti-sep">&middot;</span></span>
        <%   }
           } %>
      </div>
    </div>
  </div>
  <% } %>

  <nav>
    <a class="nav-logo" href="<%= ctx %>/" style="text-decoration:none">C3S<span>.ai</span><span class="nav-pipe"> | </span><span class="nav-uw">United Way NEFL</span></a>
    <div class="nav-actions">
      <%-- the admin dashboard, same app, its own sign-in --%>
      <a class="nav-admin" href="<%= ctx %><%= Routes.ADMIN_PREFIX %>" style="text-decoration:none;text-align:center">+ Admin</a>
      <%-- no report page yet --%>
      <button class="nav-admin" type="button" style="border-color:var(--uw-red);color:var(--uw-red);">&#128202; Report</button>
      <% if (signedInUser == null) { %>
        <a class="nav-cta" href="<%= ctx %><%= Routes.BROWSE_LOGIN %>" style="text-decoration:none;text-align:center">Sign in</a>
      <% } else { %>
        <span style="font-size:12.5px;color:var(--muted)">Signed in as <strong style="color:var(--dark);font-weight:600"><%= HtmlUtil.esc(signedInName) %></strong></span>
        <form method="post" action="<%= ctx %><%= Routes.BROWSE_LOGOUT %>" style="display:flex">
          <%= csrfInput %>
          <button class="nav-cta" type="submit">Sign out</button>
        </form>
      <% } %>
    </div>
  </nav>

  <div class="hero">
    <div class="hero-eyebrow"><span class="ey-dot"></span>United Way Northeast Florida &middot; Giving Platform</div>
    <h1>Give directly to <em>real neighbors</em> in need.</h1>
    <p>Browse verified profiles, choose who you'd like to support, and make a secure one-time or recurring donation. 100% goes directly to the recipient.</p>
  </div>

  <div class="stats-wrap">
    <div class="stats-inner">
      <div class="stat-block"><div class="stat-num"><span><%= HtmlUtil.num(stats.livesSupported()) %></span></div><div class="stat-desc">Lives supported</div></div>
      <div class="stat-block"><div class="stat-num">$<span><%= HtmlUtil.compactMoney(stats.totalRaised()) %></span></div><div class="stat-desc">Total raised this year</div></div>
      <div class="stat-block"><div class="stat-num"><span><%= HtmlUtil.num(stats.activeDonors()) %></span></div><div class="stat-desc">Active donors</div></div>
      <div class="stat-block"><div class="stat-num">$<span><%= HtmlUtil.compactMoney(stats.raisedThisWeek()) %></span></div><div class="stat-desc">Raised this week</div></div>
      <div class="stat-block"><div class="stat-num"><span><%= HtmlUtil.num(stats.newDonorsThisMonth()) %></span></div><div class="stat-desc">New donors this month</div></div>
    </div>
  </div>

  <% if (!catTotals.isEmpty()) { %>
  <div class="progress-strip">
    <span class="ps-label">Raised by category:</span>
    <% for (CategoryTotal c : catTotals) { %>
      <div class="ps-item">
        <span class="ps-cat"><%= HtmlUtil.esc(c.emoji()) %> <%= HtmlUtil.esc(c.label()) %></span>
        <div class="ps-bar-wrap"><div class="ps-bar" style="background:<%= HtmlUtil.esc(c.color()) %>;width:<%= c.barPct() %>%"></div></div>
        <span class="ps-amt" style="color:<%= HtmlUtil.esc(c.color()) %>">$<%= HtmlUtil.compactMoney(c.raised()) %></span>
      </div>
    <% } %>
  </div>
  <% } %>

  <div class="filter-section">
    <span class="filter-label">Category:</span>
    <button class="chip cat-all active" type="button" data-cat-id="all" data-filter>All</button>
    <% for (CategoryChip chip : chips) { %>
      <button class="chip <%= HtmlUtil.esc(chip.chipClass()) %>" type="button" data-cat-id="<%= chip.id() %>" data-filter><%= HtmlUtil.esc(chip.emoji()) %> <%= HtmlUtil.esc(chip.label()) %></button>
    <% } %>
    <div class="filter-right">
      <select class="sort-select" id="sort-select" aria-label="Sort profiles">
        <option value="default">Sort: Featured</option>
        <option value="pct-hi">Most funded</option>
        <option value="pct-lo">Needs most help</option>
        <option value="raised">Amount raised</option>
      </select>
      <div class="search-box">
        <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true"><circle cx="11" cy="11" r="7"/><path d="M21 21l-4.35-4.35"/></svg>
        <input type="text" id="search-input" placeholder="Search stories&hellip;" autocomplete="off" aria-label="Search stories"/>
      </div>
    </div>
  </div>

  <div class="grid-section">
    <div class="grid-header">
      <span class="grid-title">People to support</span>
      <span class="grid-count">Showing <span id="grid-shown"><%= shown %></span> of <%= cards.size() %></span>
    </div>
    <div class="donor-grid" id="donor-grid">
      <% for (ProfileCard p : cards) {
           String searchText = (p.name() + " " + p.blurb() + " " + p.tagLabel()).toLowerCase(); %>
        <article class="dcard"
                 data-cat="<%= HtmlUtil.esc(p.categoryIdsAttr()) %>"
                 data-pct="<%= p.percent() %>"
                 data-raised="<%= p.raised() %>"
                 data-search="<%= HtmlUtil.esc(searchText) %>">
          <div class="dcard-img" style="background:<%= HtmlUtil.esc(p.cardBackground()) %>">
            <%= HtmlUtil.esc(p.emoji()) %>
            <% if (p.isWeekly()) { %><span class="dcard-weekly">&#128293; Top this week</span><% } %>
          </div>
          <div class="dcard-body">
            <div class="dcard-name"><%= HtmlUtil.esc(p.name()) %></div>
            <span class="dcard-tag <%= HtmlUtil.esc(p.tagClass()) %>"><%= HtmlUtil.esc(p.tagLabel()) %></span>
            <div class="dcard-blurb"><%= HtmlUtil.esc(p.blurb()) %></div>
            <div class="dcard-progress">
              <div class="dcard-prog-row"><span class="dcard-prog-label">$<%= HtmlUtil.num(p.raised()) %> of $<%= HtmlUtil.num(p.goal()) %></span><span class="dcard-prog-pct"><%= p.percent() %>%</span></div>
              <div class="prog-track"><div class="prog-fill" style="width:<%= p.percent() %>%"></div></div>
            </div>
            <% if (p.isWeekly()) { %><div class="dcard-weekly-donor">+$<%= HtmlUtil.num(p.weeklyRaised()) %> raised this week</div><% } %>
            <%-- profile page not built yet --%>
            <button class="dcard-btn" type="button">View profile &amp; donate</button>
          </div>
        </article>
      <% } %>
    </div>
  </div>

  <div class="page-footer"><strong>100%</strong> of every donation goes directly to the recipient &middot; United Way NEFL covers all platform fees &middot; Payments secured by PayPal</div>
</div>

</body>
</html>
