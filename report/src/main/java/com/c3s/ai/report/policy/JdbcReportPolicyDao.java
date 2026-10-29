package com.c3s.ai.report.policy;

import com.c3s.ai.common.db.JdbcReader;

/** Read-only: extends JdbcReader, which has no write helpers. */
final class JdbcReportPolicyDao extends JdbcReader implements ReportPolicyDao {

    // the default policy is the row with no category (id 1 in schema.sql)
    private static final String DEFAULT_POLICY_SQL = """
            SELECT s.fee_model, s.fee_pct, s.fee_fixed,
                   COALESCE((SELECT SUM(b.pct) FROM uwnefl_allocation_buckets b
                             WHERE b.settings_id = s.id AND b.to_recipient = 0), 0) AS unrestricted_pct
            FROM uwnefl_allocation_settings s
            WHERE s.category_id IS NULL
            ORDER BY s.id
            LIMIT 1
            """;

    @Override
    public ReportPolicy defaultPolicy() {
        return queryOptional("Load default allocation policy", DEFAULT_POLICY_SQL, rs -> new ReportPolicy(
                FeeModel.fromDbValue(rs.getString("fee_model")), rs.getBigDecimal("fee_pct"),
                rs.getBigDecimal("fee_fixed"), rs.getInt("unrestricted_pct")))
                .orElse(ReportPolicy.FALLBACK);
    }
}
