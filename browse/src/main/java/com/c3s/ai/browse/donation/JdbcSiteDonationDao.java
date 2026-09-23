package com.c3s.ai.browse.donation;

import com.c3s.ai.common.db.JdbcReader;

import java.util.List;

final class JdbcSiteDonationDao extends JdbcReader implements SiteDonationDao {

    // each anonymous donation counts as a separate donor
    private static final String DONOR_IDENTITY =
            "COALESCE(CAST(d.user_id AS CHAR), CONCAT('anon:', d.id))";

    private static final String TICKER_SQL = """
            SELECT d.amount, u.display_name, p.name AS recipient
            FROM uwnefl_donations d
            JOIN uwnefl_profiles p ON p.id = d.profile_id AND p.status = 'published'
            LEFT JOIN uwnefl_users u ON u.id = d.user_id
            WHERE d.donated_at >= NOW() - INTERVAL 7 DAY
            ORDER BY d.donated_at DESC, d.id DESC
            LIMIT ?
            """;

    private static final String ACTIVE_DONORS_SQL = """
            SELECT COUNT(DISTINCT %s)
            FROM uwnefl_donations d
            JOIN uwnefl_profiles p ON p.id = d.profile_id AND p.status = 'published'
            """.formatted(DONOR_IDENTITY);

    private static final String RAISED_THIS_WEEK_SQL = """
            SELECT COALESCE(SUM(d.amount), 0)
            FROM uwnefl_donations d
            JOIN uwnefl_profiles p ON p.id = d.profile_id AND p.status = 'published'
            WHERE d.donated_at >= NOW() - INTERVAL 7 DAY
            """;

    // donors whose first donation was this month
    private static final String NEW_DONORS_THIS_MONTH_SQL = """
            SELECT COUNT(*) FROM (
                SELECT %s AS donor, MIN(d.donated_at) AS first_at
                FROM uwnefl_donations d
                JOIN uwnefl_profiles p ON p.id = d.profile_id AND p.status = 'published'
                GROUP BY donor
                HAVING first_at >= DATE_FORMAT(NOW(), '%%Y-%%m-01')
            ) t
            """.formatted(DONOR_IDENTITY);

    @Override
    public List<TickerItem> recentTicker(int limit) {
        return queryList("Load donation ticker", TICKER_SQL, rs -> {
            String name = rs.getString("display_name");
            return new TickerItem(name != null ? name : "Anonymous", rs.getLong("amount"), rs.getString("recipient"));
        }, Math.max(1, limit));
    }

    @Override
    public DonorStats donorStats() {
        return read("Load donor stats", r -> new DonorStats(
                r.scalarLong(ACTIVE_DONORS_SQL),
                r.scalarLong(RAISED_THIS_WEEK_SQL),
                r.scalarLong(NEW_DONORS_THIS_MONTH_SQL)));
    }
}
