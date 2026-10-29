package com.c3s.ai.browse.dao;

import com.c3s.ai.browse.db.DBUtil;
import com.c3s.ai.browse.exception.DataAccessException;
import com.c3s.ai.browse.model.TickerItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class JdbcDonationDao implements DonationDao {

    private static final Logger log = LoggerFactory.getLogger(JdbcDonationDao.class);

    // each anonymous donation counts as a separate donor
    private static final String DONOR_IDENTITY =
            "COALESCE(CAST(d.user_id AS CHAR), CONCAT('anon:', d.id))";

    private static final String TICKER_SQL = """
            SELECT d.amount, u.display_name, p.name AS recipient
            FROM uwnefl_donations d
            JOIN uwnefl_profiles p ON p.id = d.profile_id AND p.status = 'published'
            LEFT JOIN uwnefl_browse_users u ON u.id = d.user_id
            WHERE d.donated_at >= NOW() - INTERVAL 7 DAY
            ORDER BY d.donated_at DESC, d.id DESC
            LIMIT ?
            """;

    private static final String ACTIVE_DONORS_SQL = """
            SELECT COUNT(DISTINCT %s) AS n
            FROM uwnefl_donations d
            JOIN uwnefl_profiles p ON p.id = d.profile_id AND p.status = 'published'
            """.formatted(DONOR_IDENTITY);

    private static final String RAISED_THIS_WEEK_SQL = """
            SELECT COALESCE(SUM(d.amount), 0) AS n
            FROM uwnefl_donations d
            JOIN uwnefl_profiles p ON p.id = d.profile_id AND p.status = 'published'
            WHERE d.donated_at >= NOW() - INTERVAL 7 DAY
            """;

    // donors whose first donation was this month
    private static final String NEW_DONORS_THIS_MONTH_SQL = """
            SELECT COUNT(*) AS n FROM (
                SELECT %s AS donor, MIN(d.donated_at) AS first_at
                FROM uwnefl_donations d
                JOIN uwnefl_profiles p ON p.id = d.profile_id AND p.status = 'published'
                GROUP BY donor
                HAVING first_at >= DATE_FORMAT(NOW(), '%%Y-%%m-01')
            ) t
            """.formatted(DONOR_IDENTITY);

    @Override
    public List<TickerItem> recentTicker(int limit) {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(TICKER_SQL)) {
            stmt.setInt(1, Math.max(1, limit));
            try (ResultSet rs = stmt.executeQuery()) {
                List<TickerItem> out = new ArrayList<>();
                while (rs.next()) {
                    String name = rs.getString("display_name");
                    out.add(new TickerItem(name != null ? name : "Anonymous",
                            rs.getLong("amount"), rs.getString("recipient")));
                }
                return out;
            }
        } catch (SQLException e) {
            log.error("Failed to load donation ticker", e);
            throw new DataAccessException("Failed to load donation ticker", e);
        }
    }

    @Override
    public DonorStats donorStats() {
        try (Connection conn = DBUtil.getConnection()) {
            long active = scalar(conn, ACTIVE_DONORS_SQL);
            long week = scalar(conn, RAISED_THIS_WEEK_SQL);
            long month = scalar(conn, NEW_DONORS_THIS_MONTH_SQL);
            return new DonorStats(active, week, month);
        } catch (SQLException e) {
            log.error("Failed to load donor stats", e);
            throw new DataAccessException("Failed to load donor stats", e);
        }
    }

    private static long scalar(Connection conn, String sql) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getLong("n") : 0L;
        }
    }
}
