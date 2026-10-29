<%--
  The financial report from reference/report.html, on its stylesheet (report.css) rather than
  app.css. For js/report.js's what-if controls: data-field is a figure it rewrites, data-cell the
  same within a recipient row, and data-when a part shown for one fee model only.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.c3s.ai.report.page.*, com.c3s.ai.report.donation.PeriodTotals, com.c3s.ai.report.donation.RecipientTotal, com.c3s.ai.report.policy.FeeModel, com.c3s.ai.report.policy.ReportPolicy, com.c3s.ai.core.web.HtmlUtil, com.c3s.ai.core.web.Routes, java.math.BigDecimal" %>
<%
    ReportPageData data = (ReportPageData) request.getAttribute("ReportPageData");
    String ctx = request.getContextPath();

    ReportPageData.Labels labels = data.getLabels();
    ReportPolicy policy = data.getPolicy();
    PeriodTotals t = data.getTotals();
    BigDecimal charge = t.charge();
    boolean absorbed = policy.feeModel() == FeeModel.ABSORBED;
    int unresPct = policy.unrestrictedPct();
    String feeRate = ReportFormat.rate(policy.feePct()) + "% + " + ReportFormat.money(policy.feeFixed());

    double feeShare = ReportFormat.pctOf(t.processing(), charge);
    double resShare = ReportFormat.pctOf(t.restricted(), charge);
    double unresShare = ReportFormat.pctOf(t.unrestricted(), charge);
    String feeCents = ReportFormat.perDollar(t.processing(), charge);
    String resCents = ReportFormat.perDollar(t.restricted(), charge);
    String unresCents = ReportFormat.perDollar(t.unrestricted(), charge);

    Double yoy = data.getYoyPct();

    // before any output - it may touch the session
    String csrfInput = HtmlUtil.csrfInput(request);

    String pageTitle = "Financial Report — " + labels.month();
    String[] scripts = {"core.js", "progress-bars.js", "fee-split.js", "report.js"};
    String stylesheet = "report.css";
    boolean brandSticky = false;
    String brandLabel = "FINANCIAL REPORT";
%>
<!DOCTYPE html>
<html lang="en">
<head>
<%@ include file="/WEB-INF/views/common/head.jspf" %>
</head>
<body>

<div class="page active" id="page-report"
     data-fee-pct="<%= HtmlUtil.esc(policy.feePct().toPlainString()) %>"
     data-fee-fixed="<%= HtmlUtil.esc(policy.feeFixed().toPlainString()) %>"
     data-min-label-pct="<%= ReportFormat.MIN_LABELLED_SEGMENT_PCT %>">
<%@ include file="/WEB-INF/views/common/brand.jspf" %>
  <div class="report-page">
    <div class="report-topbar">
      <div class="report-topbar-title" style="font-family:'Antonio',sans-serif;">📊 Financial Report — <%= HtmlUtil.esc(labels.month()) %></div>
      <div class="report-topbar-actions">
        <%-- a form, not a link: report.css (the reference's, unchanged) styles .tb-btn for buttons only --%>
        <form method="get" action="<%= ctx %><%= Routes.BROWSE_HOME %>"><button type="submit" class="tb-btn tb-btn-outline">← Back to site</button></form>
        <button type="button" class="tb-btn tb-btn-primary" data-print="data-print">Export PDF</button>
        <%-- ends the report scope only; a dashboard or donor session stays signed in --%>
        <form method="post" action="<%= ctx %><%= Routes.REPORT_LOGOUT %>"><%= csrfInput %><button type="submit" class="tb-btn tb-btn-outline">Sign out</button></form>
      </div>
    </div>
    <div class="report-content">

      <%-- KPI row --%>
      <div class="report-kpi-row" id="report-kpi-row">
        <div class="kpi-card blue">
          <div class="kpi-label">Total Collected</div>
          <div class="kpi-value" data-field="charge"><%= ReportFormat.money0(charge) %></div>
          <div class="kpi-sub"><%= ReportFormat.num(t.transactions()) %> transactions · YTD through <%= HtmlUtil.esc(labels.asOf()) %></div>
          <% if (yoy != null) { %>
          <div class="kpi-badge <%= yoy >= 0 ? "up" : "down" %>"><%= yoy >= 0 ? "↑" : "↓" %> <%= ReportFormat.pct1(Math.abs(yoy)) %> vs last year</div>
          <% } %>
        </div>
        <div class="kpi-card red">
          <div class="kpi-label">Processing Fees</div>
          <div class="kpi-value" style="color:var(--alloc-fee)" data-field="processing"><%= ReportFormat.money0(t.processing()) %></div>
          <div class="kpi-sub"><%= HtmlUtil.esc(feeRate) %> per transaction</div>
          <div class="kpi-badge down" data-field="processing-badge"><%= feeCents %> of every $1</div>
        </div>
        <div class="kpi-card green">
          <div class="kpi-label">→ Restricted Account</div>
          <div class="kpi-value" style="color:var(--alloc-res)" data-field="restricted"><%= ReportFormat.money0(t.restricted()) %></div>
          <div class="kpi-sub">Donor-designated to named recipients</div>
          <div class="kpi-badge up" data-field="restricted-badge"><%= resCents %> of every $1</div>
        </div>
        <div class="kpi-card gold">
          <div class="kpi-label">→ Unrestricted Account</div>
          <div class="kpi-value" style="color:var(--alloc-unres)" data-field="unrestricted"><%= ReportFormat.money0(t.unrestricted()) %></div>
          <div class="kpi-sub">UWNEFL general operating fund</div>
          <div class="kpi-badge up" data-field="unrestricted-badge"><%= unresCents %> of every $1</div>
        </div>
      </div>

      <%-- ══ FUND ALLOCATION ══ --%>
      <div class="report-alloc-grid">
        <div class="report-card">
          <div class="rc-header">
            <div><div class="rc-title">Where Every Dollar Goes</div><div class="rc-sub">split applied at the moment of settlement</div></div>
          </div>
          <div class="rc-body" style="padding:1.1rem 1.4rem 1.4rem;">
            <div id="report-alloc-flow">
              <div class="df-wrap">
                <div class="df-coin">
                  <div class="df-coin-amt">$1.00</div>
                  <div class="df-coin-lbl">every dollar</div>
                </div>
                <div class="df-stem"></div>
                <div class="df-rail"></div>
                <div class="df-buckets">
                  <div class="df-bucket"><div class="df-card fee">
                    <div class="df-icon">💳</div>
                    <div class="df-amt" data-field="processing-cents"><%= feeCents %></div>
                    <div class="df-name">Processing fee</div>
                    <div class="df-desc"><span data-when="absorbed"<%= absorbed ? "" : " hidden=\"hidden\"" %>>Nothing taken from the gift — UWNEFL paid <span data-field="fee"><%= ReportFormat.money0(t.fee()) %></span> from its general fund</span><span data-when="charged"<%= absorbed ? " hidden=\"hidden\"" : "" %>>Payment processor · <%= HtmlUtil.esc(feeRate) %> per transaction</span></div>
                    <div class="df-pct" data-field="processing-share"><%= ReportFormat.pct1(feeShare) %></div>
                  </div></div>
                  <div class="df-bucket"><div class="df-card res">
                    <div class="df-icon">🔒</div>
                    <div class="df-amt" data-field="restricted-cents"><%= resCents %></div>
                    <div class="df-name">Restricted account</div>
                    <div class="df-desc">Donor-restricted — released only to the named recipient</div>
                    <div class="df-pct" data-field="restricted-share"><%= ReportFormat.pct1(resShare) %></div>
                  </div></div>
                  <div class="df-bucket"><div class="df-card unres">
                    <div class="df-icon">🏛️</div>
                    <div class="df-amt" data-field="unrestricted-cents"><%= unresCents %></div>
                    <div class="df-name">Unrestricted account</div>
                    <div class="df-desc">UWNEFL general fund — programs, staffing, operations</div>
                    <div class="df-pct" data-field="unrestricted-share"><%= ReportFormat.pct1(unresShare) %></div>
                  </div></div>
                </div>
              </div>
            </div>
            <div class="fee-model" style="border-top:1px solid var(--border-light);margin-top:16px;">
              <span class="fee-model-lbl">Demo controls · fee handling</span>
              <div class="fee-seg">
                <% for (FeeModel model : FeeModel.values()) { %>
                <button type="button" data-model="<%= model.dbValue() %>" data-label="<%= HtmlUtil.esc(model.label()) %>"<%= model == policy.feeModel() ? " class=\"active\"" : "" %>><%= HtmlUtil.esc(model.buttonLabel()) %></button>
                <% } %>
              </div>
            </div>
            <div class="alloc-ctrl" style="border-top:1px dashed var(--border);">
              <div class="alloc-ctrl-top"><span>Unrestricted share of each net gift</span><span class="alloc-ctrl-val" id="rp-slider-val"><%= unresPct %>%</span></div>
              <%-- the reference stops at 50%; go further when the policy already does --%>
              <input type="range" class="alloc-slider" id="rp-slider" min="0" max="<%= Math.max(50, unresPct) %>" step="1" value="<%= unresPct %>"/>
              <div class="alloc-ctrl-hint">Adjust to model different allocation policies — every figure on this page recalculates live.</div>
            </div>
          </div>
        </div>
        <div class="report-card">
          <div class="rc-header"><div><div class="rc-title">Account Destinations — YTD</div><div class="rc-sub">gross collections by receiving account</div></div></div>
          <div class="rc-body" style="padding:1.1rem 1.4rem 1.4rem;">
            <div id="report-alloc-bar">
              <div class="alloc-bar">
                <div class="alloc-seg fee" id="rp-seg-fee" data-bar-pct="<%= ReportFormat.barPct(feeShare) %>" data-bar-min-px="<%= feeShare > 0 ? 3 : 0 %>"><%= ReportFormat.segmentLabel(feeShare) %></div>
                <div class="alloc-seg res" id="rp-seg-res" data-bar-pct="<%= ReportFormat.barPct(resShare) %>" data-bar-min-px="<%= resShare > 0 ? 3 : 0 %>"><%= ReportFormat.segmentLabel(resShare) %></div>
                <div class="alloc-seg unres" id="rp-seg-unres" data-bar-pct="<%= ReportFormat.barPct(unresShare) %>" data-bar-min-px="<%= unresShare > 0 ? 3 : 0 %>"><%= ReportFormat.segmentLabel(unresShare) %></div>
              </div>
              <div class="alloc-scale"><span>$0</span><span><span data-field="charge"><%= ReportFormat.money0(charge) %></span> collected YTD</span></div>
              <div class="alloc-rows">
                <div class="alloc-row"><span class="alloc-dot fee"></span>
                  <span class="alloc-lbl">Processing fee<small>Payment processor settlement</small></span>
                  <span class="alloc-right"><span class="alloc-amt fee" data-field="processing"><%= ReportFormat.money0(t.processing()) %></span>
                  <span class="alloc-cents" data-field="processing-per"><%= feeCents %> per $1</span></span></div>
                <div class="alloc-row"><span class="alloc-dot res"></span>
                  <span class="alloc-lbl">Restricted account<small>Released against recipient goals only</small></span>
                  <span class="alloc-right"><span class="alloc-amt res" data-field="restricted"><%= ReportFormat.money0(t.restricted()) %></span>
                  <span class="alloc-cents" data-field="restricted-per"><%= resCents %> per $1</span></span></div>
                <div class="alloc-row"><span class="alloc-dot unres"></span>
                  <span class="alloc-lbl">Unrestricted account<small>General fund — board-designated use</small></span>
                  <span class="alloc-right"><span class="alloc-amt unres" data-field="unrestricted"><%= ReportFormat.money0(t.unrestricted()) %></span>
                  <span class="alloc-cents" data-field="unrestricted-per"><%= unresCents %> per $1</span></span></div>
              </div>
              <div class="alloc-total-row"><span>Total collected YTD</span><strong data-field="charge"><%= ReportFormat.money0(charge) %></strong></div>
            </div>
          </div>
        </div>
      </div>

      <%-- Allocation detail table --%>
      <div class="report-card" style="margin-bottom:16px;">
        <div class="rc-header"><div><div class="rc-title">Fund Allocation by Recipient</div><div class="rc-sub">three-way split · processing fee, restricted account, unrestricted account</div></div></div>
        <table class="fin-table">
          <thead>
            <tr>
              <th>Recipient / Category</th>
              <th>Collected</th>
              <th>Transactions</th>
              <th>Processing Fee</th>
              <th>→ Restricted A/C</th>
              <th>→ Unrestricted A/C</th>
            </tr>
          </thead>
          <tbody id="alloc-table-body">
            <% for (RecipientTotal r : data.getRecipients()) { %>
            <tr data-gross="<%= r.gross() %>" data-tx="<%= r.transactions() %>">
              <td><strong><%= HtmlUtil.esc(r.name()) %></strong><br/><span style="font-size:11px;color:var(--muted)"><%= HtmlUtil.esc(ReportFormat.category(r.categoryEmoji(), r.categoryLabel())) %></span></td>
              <td class="td-bold" data-cell="charge"><%= ReportFormat.money0(r.charge()) %></td>
              <td><%= r.transactions() %></td>
              <td class="td-red" data-cell="processing"><%= ReportFormat.deduction(r.processing()) %></td>
              <td class="td-green" data-cell="restricted"><%= ReportFormat.money0(r.restricted()) %></td>
              <td class="td-gold" data-cell="unrestricted"><%= ReportFormat.money0(r.unrestricted()) %></td>
            </tr>
            <% } %>
            <tr class="total-row">
              <td>TOTAL (YTD <%= HtmlUtil.esc(labels.year()) %>)</td>
              <td class="td-bold" data-field="charge"><%= ReportFormat.money0(charge) %></td>
              <td><%= ReportFormat.num(t.transactions()) %></td>
              <td class="td-red" data-field="processing-deduction"><%= ReportFormat.deduction(t.processing()) %></td>
              <td class="td-green" style="font-family:'Antonio',sans-serif;font-size:15px;" data-field="restricted"><%= ReportFormat.money0(t.restricted()) %></td>
              <td class="td-gold" style="font-family:'Antonio',sans-serif;font-size:15px;" data-field="unrestricted"><%= ReportFormat.money0(t.unrestricted()) %></td>
            </tr>
          </tbody>
        </table>
        <div class="fin-note" id="alloc-table-note">ⓘ Split policy in effect: <strong data-field="model-label"><%= HtmlUtil.esc(policy.feeModel().label()) %></strong> ·
          processing <%= HtmlUtil.esc(feeRate) %> per transaction ·
          <strong data-field="restricted-pct"><%= 100 - unresPct %>%</strong> of each net gift to the restricted account,
          <strong data-field="unrestricted-pct"><%= unresPct %>%</strong> to the unrestricted general fund.
          <span data-when="absorbed"<%= absorbed ? "" : " hidden=\"hidden\"" %>>Processing fees of <span data-field="fee"><%= ReportFormat.money0(t.fee()) %></span> were paid by UWNEFL out of unrestricted funds and are not deducted from donor gifts. </span>
          Restricted balances are released against verified recipient need; unrestricted revenue is board-designated.
          Report period: <%= HtmlUtil.esc(labels.period()) %>.</div>
      </div>

      <%-- Weekly chart + category breakdown --%>
      <div class="report-two-col">
        <div class="report-card">
          <div class="rc-header"><div class="rc-title">Weekly Donation Volume</div><div class="rc-sub">$ raised each week · current week highlighted</div></div>
          <div class="rc-body">
            <div class="weekly-chart" id="weekly-chart">
              <% for (WeekBar w : data.getWeeks()) { %>
              <div class="wc-bar-wrap">
                <div class="wc-bar-val"><%= HtmlUtil.esc(w.valueLabel()) %></div>
                <div class="wc-bar <%= w.current() ? "highlight" : "" %>" style="height:<%= w.heightPx() %>px"></div>
                <div class="wc-day"><%= HtmlUtil.esc(w.labelTop()) %><br/><%= HtmlUtil.esc(w.labelBottom()) %></div>
              </div>
              <% } %>
            </div>
            <div style="display:flex;gap:14px;font-size:11px;color:var(--muted);margin-top:4px;">
              <span style="display:flex;align-items:center;gap:4px;"><span style="width:10px;height:10px;background:var(--uw-blue);border-radius:2px;display:inline-block;"></span>Past weeks</span>
              <span style="display:flex;align-items:center;gap:4px;"><span style="width:10px;height:10px;background:var(--uw-gold);border-radius:2px;display:inline-block;"></span>Current week</span>
            </div>
          </div>
        </div>
        <div class="report-card">
          <div class="rc-header"><div class="rc-title">Amount Raised by Category</div><div class="rc-sub">YTD <%= HtmlUtil.esc(labels.year()) %></div></div>
          <div class="rc-body">
            <div class="cat-breakdown">
              <% for (CategoryRow c : data.getCategories()) { %>
              <div class="cb-row"><span class="cb-emoji"><%= HtmlUtil.esc(c.emoji()) %></span><span class="cb-label"><%= HtmlUtil.esc(c.label()) %></span><div class="cb-bar-wrap"><div class="cb-bar" style="background:<%= c.color() %>;width:<%= c.barPct() %>%"></div></div><span class="cb-amt" style="color:<%= c.color() %>"><%= HtmlUtil.esc(c.amount()) %></span><span class="cb-pct"><%= HtmlUtil.esc(c.share()) %></span></div>
              <% } %>
              <% if (data.getCategories().isEmpty()) { %>
              <div class="rc-sub">No donations yet this year.</div>
              <% } %>
            </div>
          </div>
        </div>
      </div>

      <%-- Top donors table --%>
      <div class="report-card" style="margin-bottom:16px;">
        <div class="rc-header"><div class="rc-title">Top Donors This Month</div><div class="rc-sub"><%= HtmlUtil.esc(labels.month()) %> · ranked by total given</div></div>
        <table class="donor-table">
          <thead><tr><th>#</th><th>Donor</th><th>Recipient</th><th>Category</th><th>Amount</th><th>Type</th><th>Date</th></tr></thead>
          <tbody>
            <% for (DonorRow d : data.getDonors()) { %>
            <tr><td><span class="donor-rank<%= d.rankClass().isEmpty() ? "" : " " + d.rankClass() %>"<%= d.rankColor().isEmpty() ? "" : " style=\"color:" + d.rankColor() + ";\"" %>><%= d.rank() %></span></td><td class="donor-name-cell"><%= HtmlUtil.esc(d.donor()) %></td><td><%= HtmlUtil.esc(d.recipient()) %></td><td><span class="donor-cat-badge<%= d.tagClass().isEmpty() ? "" : " " + d.tagClass() %>"><%= HtmlUtil.esc(d.category()) %></span></td><td class="td-bold td-green"><%= HtmlUtil.esc(d.amount()) %></td><td><%= HtmlUtil.esc(d.type()) %></td><td><%= HtmlUtil.esc(d.date()) %></td></tr>
            <% } %>
            <% if (data.getDonors().isEmpty()) { %>
            <tr><td colspan="7">No donations yet this month.</td></tr>
            <% } %>
          </tbody>
        </table>
      </div>

    </div>
  </div>
</div>

</body>
</html>
