package com.c3s.ai.admin.allocation;

import com.c3s.ai.common.db.JdbcWriter;
import com.c3s.ai.common.db.RowMapper;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;

final class JdbcAllocationDao extends JdbcWriter implements AllocationDao {

    private static final String SETTINGS_COLUMNS = "id, category_id, fee_model, fee_pct, fee_fixed, allow_donor_adjust";

    private static final String SELECT_DEFAULT_SQL =
            "SELECT " + SETTINGS_COLUMNS + " FROM uwnefl_allocation_settings WHERE category_id IS NULL";
    private static final String SELECT_OVERRIDE_SQL =
            "SELECT " + SETTINGS_COLUMNS + " FROM uwnefl_allocation_settings WHERE category_id = ?";
    private static final String SELECT_BUCKETS_SQL = """
            SELECT bucket_key, name, pct, to_recipient, core, description, purpose, sort_order
            FROM uwnefl_allocation_buckets
            WHERE settings_id = ?
            ORDER BY sort_order ASC, id ASC
            """;
    private static final String SELECT_OVERRIDE_CATEGORY_IDS_SQL =
            "SELECT category_id FROM uwnefl_allocation_settings WHERE category_id IS NOT NULL";
    private static final String INSERT_SETTINGS_SQL =
            "INSERT INTO uwnefl_allocation_settings (category_id, fee_model, fee_pct, fee_fixed, allow_donor_adjust) VALUES (?, ?, ?, ?, ?)";
    private static final String UPDATE_SETTINGS_SQL =
            "UPDATE uwnefl_allocation_settings SET fee_model = ?, fee_pct = ?, fee_fixed = ?, allow_donor_adjust = ? WHERE id = ?";
    private static final String DELETE_BUCKETS_SQL = "DELETE FROM uwnefl_allocation_buckets WHERE settings_id = ?";
    private static final String INSERT_BUCKET_SQL = """
            INSERT INTO uwnefl_allocation_buckets
                (settings_id, bucket_key, name, pct, to_recipient, core, description, purpose, sort_order)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final RowMapper<AllocationSettings> SETTINGS = rs -> {
        AllocationSettings s = new AllocationSettings();
        s.setId(rs.getLong("id"));
        long categoryId = rs.getLong("category_id");
        s.setCategoryId(rs.wasNull() ? null : categoryId);
        s.setFeeModel(FeeModel.fromDbValue(rs.getString("fee_model")));
        s.setFeePct(rs.getBigDecimal("fee_pct"));
        s.setFeeFixed(rs.getBigDecimal("fee_fixed"));
        s.setAllowDonorAdjust(rs.getBoolean("allow_donor_adjust"));
        return s;
    };

    private static final RowMapper<AllocationBucket> BUCKET = rs -> {
        AllocationBucket b = new AllocationBucket();
        b.setBucketKey(rs.getString("bucket_key"));
        b.setName(rs.getString("name"));
        b.setPct(rs.getInt("pct"));
        b.setToRecipient(rs.getBoolean("to_recipient"));
        b.setCore(rs.getBoolean("core"));
        b.setDescription(rs.getString("description"));
        b.setPurpose(rs.getString("purpose"));
        b.setSortOrder(rs.getInt("sort_order"));
        return b;
    };

    @Override
    public Optional<AllocationSettings> findDefault() {
        return queryOptional("Load default allocation settings", SELECT_DEFAULT_SQL, SETTINGS);
    }

    @Override
    public Optional<AllocationSettings> findOverride(long categoryId) {
        return queryOptional("Load allocation override", SELECT_OVERRIDE_SQL, SETTINGS, categoryId);
    }

    @Override
    public List<AllocationBucket> findBuckets(long settingsId) {
        return queryList("Load allocation buckets", SELECT_BUCKETS_SQL, BUCKET, settingsId);
    }

    @Override
    public Set<Long> findOverrideCategoryIds() {
        return new HashSet<>(queryList("Load allocation override category ids",
                SELECT_OVERRIDE_CATEGORY_IDS_SQL, rs -> rs.getLong(1)));
    }

    @Override
    public OptionalLong saveSettingsAndBuckets(AllocationSettings settings, List<AllocationBucket> buckets) {
        return inTransaction("Save allocation settings", w -> {
            long id;
            if (settings.getId() != null) {
                id = settings.getId();
                if (w.update(UPDATE_SETTINGS_SQL, settings.getFeeModel(), settings.getFeePct(), settings.getFeeFixed(),
                        settings.isAllowDonorAdjust(), id) == 0) {
                    return OptionalLong.empty();
                }
            } else {
                id = w.insertReturningKey(INSERT_SETTINGS_SQL, settings.getCategoryId(), settings.getFeeModel(),
                        settings.getFeePct(), settings.getFeeFixed(), settings.isAllowDonorAdjust());
            }
            w.update(DELETE_BUCKETS_SQL, id);
            int[] sortOrder = {0};
            w.batch(INSERT_BUCKET_SQL, buckets, b -> new Object[]{id, b.getBucketKey(), b.getName(), b.getPct(),
                    b.isToRecipient(), b.isCore(), b.getDescription(), b.getPurpose(), sortOrder[0]++});
            return OptionalLong.of(id);
        });
    }
}
