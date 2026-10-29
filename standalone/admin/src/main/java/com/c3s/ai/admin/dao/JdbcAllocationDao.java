package com.c3s.ai.admin.dao;

import com.c3s.ai.admin.db.DBUtil;
import com.c3s.ai.admin.exception.DataAccessException;
import com.c3s.ai.admin.exception.EntityNotFoundException;
import com.c3s.ai.admin.model.AllocationBucket;
import com.c3s.ai.admin.model.AllocationSettings;
import com.c3s.ai.admin.model.FeeModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JdbcAllocationDao implements AllocationDao {

    private static final Logger log = LoggerFactory.getLogger(JdbcAllocationDao.class);

    private static final String SELECT_DEFAULT_SQL = "SELECT * FROM uwnefl_allocation_settings WHERE category_id IS NULL";
    private static final String SELECT_OVERRIDE_SQL = "SELECT * FROM uwnefl_allocation_settings WHERE category_id = ?";
    private static final String SELECT_BUCKETS_SQL =
            "SELECT * FROM uwnefl_allocation_buckets WHERE settings_id = ? ORDER BY sort_order ASC, id ASC";
    private static final String INSERT_SETTINGS_SQL =
            "INSERT INTO uwnefl_allocation_settings (category_id, fee_model, fee_pct, fee_fixed, allow_donor_adjust) VALUES (?, ?, ?, ?, ?)";
    private static final String UPDATE_SETTINGS_SQL =
            "UPDATE uwnefl_allocation_settings SET fee_model = ?, fee_pct = ?, fee_fixed = ?, allow_donor_adjust = ? WHERE id = ?";
    private static final String SELECT_OVERRIDE_CATEGORY_IDS_SQL =
            "SELECT category_id FROM uwnefl_allocation_settings WHERE category_id IS NOT NULL";
    private static final String DELETE_BUCKETS_SQL = "DELETE FROM uwnefl_allocation_buckets WHERE settings_id = ?";
    private static final String INSERT_BUCKET_SQL = """
            INSERT INTO uwnefl_allocation_buckets
                (settings_id, bucket_key, name, pct, to_recipient, core, description, purpose, sort_order)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    @Override
    public AllocationSettings getDefaultSettings() {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_DEFAULT_SQL);
             ResultSet rs = stmt.executeQuery()) {
            if (!rs.next()) {
                throw new DataAccessException("Default allocation policy row is missing - re-run schema.sql");
            }
            return mapSettings(rs);
        } catch (SQLException e) {
            log.error("Failed to load default allocation settings", e);
            throw new DataAccessException("Failed to load default allocation settings", e);
        }
    }

    @Override
    public AllocationSettings getOverride(long categoryId) {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_OVERRIDE_SQL)) {
            stmt.setLong(1, categoryId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapSettings(rs) : null;
            }
        } catch (SQLException e) {
            log.error("Failed to load allocation override for categoryId={}", categoryId, e);
            throw new DataAccessException("Failed to load allocation override", e);
        }
    }

    @Override
    public List<AllocationBucket> getBuckets(long settingsId) {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BUCKETS_SQL)) {
            stmt.setLong(1, settingsId);
            try (ResultSet rs = stmt.executeQuery()) {
                List<AllocationBucket> buckets = new ArrayList<>();
                while (rs.next()) {
                    buckets.add(mapBucket(rs));
                }
                return buckets;
            }
        } catch (SQLException e) {
            log.error("Failed to load allocation buckets for settingsId={}", settingsId, e);
            throw new DataAccessException("Failed to load allocation buckets", e);
        }
    }

    @Override
    public Set<Long> getOverrideCategoryIds() {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_OVERRIDE_CATEGORY_IDS_SQL);
             ResultSet rs = stmt.executeQuery()) {
            Set<Long> ids = new HashSet<>();
            while (rs.next()) {
                ids.add(rs.getLong("category_id"));
            }
            return ids;
        } catch (SQLException e) {
            log.error("Failed to load allocation override category ids", e);
            throw new DataAccessException("Failed to load allocation override category ids", e);
        }
    }

    @Override
    public long saveSettingsAndBuckets(AllocationSettings settings, List<AllocationBucket> buckets)
            throws EntityNotFoundException {
        SaveOutcome outcome;
        try {
            outcome = DBUtil.inTransaction(conn -> {
                SaveOutcome result = saveSettings(conn, settings);
                if (result.existed()) {
                    saveBuckets(conn, result.id(), buckets);
                }
                return result;
            });
        } catch (SQLException e) {
            log.error("Failed to save allocation settings/buckets categoryId={}", settings.getCategoryId(), e);
            throw new DataAccessException("Failed to save allocation settings", e);
        }
        if (!outcome.existed()) {
            throw new EntityNotFoundException("allocation settings", outcome.id());
        }
        log.info("Saved allocation settings id={} categoryId={} with {} buckets",
                outcome.id(), settings.getCategoryId(), buckets.size());
        return outcome.id();
    }

    private record SaveOutcome(long id, boolean existed) {
    }

    private SaveOutcome saveSettings(Connection conn, AllocationSettings settings) throws SQLException {
        if (settings.getId() != null) {
            try (PreparedStatement stmt = conn.prepareStatement(UPDATE_SETTINGS_SQL)) {
                stmt.setString(1, settings.getFeeModel().dbValue());
                stmt.setBigDecimal(2, settings.getFeePct());
                stmt.setBigDecimal(3, settings.getFeeFixed());
                stmt.setBoolean(4, settings.isAllowDonorAdjust());
                stmt.setLong(5, settings.getId());
                boolean existed = stmt.executeUpdate() > 0;
                return new SaveOutcome(settings.getId(), existed);
            }
        }
        try (PreparedStatement stmt = conn.prepareStatement(INSERT_SETTINGS_SQL, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, settings.getCategoryId());
            stmt.setString(2, settings.getFeeModel().dbValue());
            stmt.setBigDecimal(3, settings.getFeePct());
            stmt.setBigDecimal(4, settings.getFeeFixed());
            stmt.setBoolean(5, settings.isAllowDonorAdjust());
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Insert did not return a generated id");
                }
                return new SaveOutcome(keys.getLong(1), true);
            }
        }
    }

    private void saveBuckets(Connection conn, long settingsId, List<AllocationBucket> buckets) throws SQLException {
        try (PreparedStatement del = conn.prepareStatement(DELETE_BUCKETS_SQL)) {
            del.setLong(1, settingsId);
            del.executeUpdate();
        }
        try (PreparedStatement ins = conn.prepareStatement(INSERT_BUCKET_SQL)) {
            int sortOrder = 0;
            for (AllocationBucket b : buckets) {
                ins.setLong(1, settingsId);
                ins.setString(2, b.getBucketKey());
                ins.setString(3, b.getName());
                ins.setInt(4, b.getPct());
                ins.setBoolean(5, b.isToRecipient());
                ins.setBoolean(6, b.isCore());
                ins.setString(7, b.getDescription());
                ins.setString(8, b.getPurpose());
                ins.setInt(9, sortOrder++);
                ins.addBatch();
            }
            ins.executeBatch();
        }
    }

    private AllocationSettings mapSettings(ResultSet rs) throws SQLException {
        AllocationSettings s = new AllocationSettings();
        s.setId(rs.getLong("id"));
        long categoryId = rs.getLong("category_id");
        s.setCategoryId(rs.wasNull() ? null : categoryId);
        s.setFeeModel(FeeModel.fromDbValue(rs.getString("fee_model")));
        s.setFeePct(rs.getBigDecimal("fee_pct"));
        s.setFeeFixed(rs.getBigDecimal("fee_fixed"));
        s.setAllowDonorAdjust(rs.getBoolean("allow_donor_adjust"));
        return s;
    }

    private AllocationBucket mapBucket(ResultSet rs) throws SQLException {
        AllocationBucket b = new AllocationBucket();
        b.setId(rs.getLong("id"));
        b.setBucketKey(rs.getString("bucket_key"));
        b.setName(rs.getString("name"));
        b.setPct(rs.getInt("pct"));
        b.setToRecipient(rs.getBoolean("to_recipient"));
        b.setCore(rs.getBoolean("core"));
        b.setDescription(rs.getString("description"));
        b.setPurpose(rs.getString("purpose"));
        b.setSortOrder(rs.getInt("sort_order"));
        return b;
    }
}
