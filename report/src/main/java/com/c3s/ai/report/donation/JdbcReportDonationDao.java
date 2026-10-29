package com.c3s.ai.report.donation;

import com.c3s.ai.common.db.JdbcReader;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Read-only: extends JdbcReader, which has no write helpers. The money comes from the settlement
 * snapshot on each donation (see schema.sql), never from the current allocation policy.
 */
final class JdbcReportDonationDao extends JdbcReader implements ReportDonationDao {

    // a profile in several categories is listed under its first
    private static final String FIRST_CATEGORY_ID =
            "(SELECT MIN(pc.category_id) FROM uwnefl_profile_categories pc WHERE pc.profile_id = %s)";

    private static final String TOTALS_SQL = """
            SELECT COUNT(*) AS tx,
                   COALESCE(SUM(d.amount), 0) AS gross,
                   COALESCE(SUM(d.charge_amount), 0) AS charge,
                   COALESCE(SUM(d.fee_amount), 0) AS fee,
                   COALESCE(SUM(d.processing_amount), 0) AS processing,
                   COALESCE(SUM(d.restricted_amount), 0) AS restricted,
                   COALESCE(SUM(d.unrestricted_amount), 0) AS unrestricted
            FROM uwnefl_donations d
            WHERE d.donated_at >= ? AND d.donated_at < ?
            """;

    private static final String RECIPIENTS_SQL = """
            SELECT p.name, c.label, c.emoji,
                   COUNT(*) AS tx,
                   SUM(d.amount) AS gross,
                   SUM(d.charge_amount) AS charge,
                   SUM(d.processing_amount) AS processing,
                   SUM(d.restricted_amount) AS restricted,
                   SUM(d.unrestricted_amount) AS unrestricted
            FROM uwnefl_donations d
            JOIN uwnefl_profiles p ON p.id = d.profile_id
            LEFT JOIN uwnefl_categories c ON c.id = %s
            WHERE d.donated_at >= ? AND d.donated_at < ?
            GROUP BY p.id, p.name, c.label, c.emoji
            ORDER BY c.label IS NULL, c.label, charge DESC, p.id
            """.formatted(FIRST_CATEGORY_ID.formatted("p.id"));

    private static final String DAYS_SQL = """
            SELECT DATE(d.donated_at) AS day, SUM(d.amount) AS amt
            FROM uwnefl_donations d
            WHERE d.donated_at >= ? AND d.donated_at < ?
            GROUP BY DATE(d.donated_at)
            ORDER BY day
            """;

    private static final String CATEGORIES_SQL = """
            SELECT c.label, c.emoji, c.color_class, SUM(d.amount) AS amt
            FROM uwnefl_donations d
            JOIN uwnefl_profile_categories pc ON pc.profile_id = d.profile_id
            JOIN uwnefl_categories c ON c.id = pc.category_id
            WHERE d.donated_at >= ? AND d.donated_at < ?
            GROUP BY c.id, c.label, c.emoji, c.color_class
            ORDER BY amt DESC, c.id
            """;

    // Each donor's total, shown against their largest gift; equal totals go to whoever gave that
    // gift first. An anonymous gift is its own donor, as in the giving page's counts. No WITH or
    // window functions (MySQL 5.7): t groups the donors, then a correlated subquery picks a named
    // donor's largest gift - an anonymous donor's only gift is anon_id.
    private static final String TOP_DONORS_SQL = """
            SELECT t.total, d.frequency, DATE(d.donated_at) AS day, u.display_name,
                   p.name AS recipient, c.label, c.emoji, c.color_class
            FROM (
                SELECT d.user_id, MIN(d.id) AS anon_id, SUM(d.amount) AS total
                FROM uwnefl_donations d
                WHERE d.donated_at >= ? AND d.donated_at < ?
                GROUP BY d.user_id, CASE WHEN d.user_id IS NULL THEN d.id END
            ) t
            JOIN uwnefl_donations d ON d.id = CASE WHEN t.user_id IS NULL THEN t.anon_id ELSE (
                SELECT d2.id FROM uwnefl_donations d2
                WHERE d2.user_id = t.user_id AND d2.donated_at >= ? AND d2.donated_at < ?
                ORDER BY d2.amount DESC, d2.donated_at DESC, d2.id DESC
                LIMIT 1) END
            JOIN uwnefl_profiles p ON p.id = d.profile_id
            LEFT JOIN uwnefl_users u ON u.id = t.user_id
            LEFT JOIN uwnefl_categories c ON c.id = %s
            ORDER BY t.total DESC, d.donated_at ASC, d.id ASC
            LIMIT ?
            """.formatted(FIRST_CATEGORY_ID.formatted("d.profile_id"));

    @Override
    public DonationReport load(ReportPeriod period, int topDonorLimit) {
        LocalDate from = period.yearStart();
        LocalDate to = period.end();
        // one read-only transaction, so the totals, the table and the charts agree with each other
        return read("Load financial report", r -> new DonationReport(
                r.optional(TOTALS_SQL, JdbcReportDonationDao::totals, from, to).orElse(PeriodTotals.EMPTY),
                r.optional(TOTALS_SQL, JdbcReportDonationDao::totals, period.lastYearStart(), period.lastYearEnd())
                        .orElse(PeriodTotals.EMPTY).charge(),
                r.list(RECIPIENTS_SQL, rs -> new RecipientTotal(
                        rs.getString("name"), rs.getString("label"), rs.getString("emoji"),
                        rs.getLong("tx"), rs.getLong("gross"), rs.getBigDecimal("charge"),
                        rs.getBigDecimal("processing"), rs.getBigDecimal("restricted"),
                        rs.getBigDecimal("unrestricted")), from, to),
                r.list(DAYS_SQL, rs -> new DayAmount(rs.getObject("day", LocalDate.class), rs.getLong("amt")),
                        period.weeksStart(), to),
                r.list(CATEGORIES_SQL, rs -> new CategoryAmount(rs.getString("label"), rs.getString("emoji"),
                        rs.getString("color_class"), rs.getLong("amt")), from, to),
                r.list(TOP_DONORS_SQL, rs -> {
                    String name = rs.getString("display_name");
                    return new TopDonor(name != null ? name : "Anonymous", rs.getLong("total"),
                            rs.getString("recipient"), rs.getString("label"), rs.getString("emoji"),
                            rs.getString("color_class"), Frequency.fromDbValue(rs.getString("frequency")),
                            rs.getObject("day", LocalDate.class));
                }, period.monthStart(), to, period.monthStart(), to, Math.max(1, topDonorLimit))));
    }

    private static PeriodTotals totals(ResultSet rs) throws SQLException {
        return new PeriodTotals(rs.getLong("tx"), rs.getLong("gross"), rs.getBigDecimal("charge"),
                rs.getBigDecimal("fee"), rs.getBigDecimal("processing"), rs.getBigDecimal("restricted"),
                rs.getBigDecimal("unrestricted"));
    }
}
