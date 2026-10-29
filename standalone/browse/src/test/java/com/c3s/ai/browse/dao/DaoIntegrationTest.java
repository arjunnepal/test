package com.c3s.ai.browse.dao;

import com.c3s.ai.browse.dao.BrowseProfileDao.CategoryRaised;
import com.c3s.ai.browse.dao.BrowseProfileDao.PublishedTotals;
import com.c3s.ai.browse.db.DBUtil;
import com.c3s.ai.browse.model.Category;
import com.c3s.ai.browse.model.Profile;
import com.c3s.ai.browse.model.TickerItem;
import com.c3s.ai.browse.model.User;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

// Loads the schema and seed into a temporary MySQL database and tests the DAOs against it.
// Skipped unless BROWSE_IT_JDBC_URL is set (a server URL without a database), e.g.
//   BROWSE_IT_JDBC_URL='jdbc:mysql://localhost:3306/?sslMode=DISABLED&serverTimezone=UTC&characterEncoding=UTF-8' \
//   BROWSE_IT_JDBC_USER=root BROWSE_IT_JDBC_PASSWORD=... ./mvnw test
class DaoIntegrationTest {

    private static final String SCHEMA = "uwnefl_it_" + Long.toHexString(System.nanoTime());

    private static String serverUrl;
    private static String user;
    private static String password;

    @BeforeAll
    static void createSchema() throws Exception {
        serverUrl = System.getenv("BROWSE_IT_JDBC_URL");
        assumeTrue(serverUrl != null && !serverUrl.isBlank(), "BROWSE_IT_JDBC_URL not set - skipping DB integration tests");
        user = System.getenv().getOrDefault("BROWSE_IT_JDBC_USER", "root");
        password = System.getenv().getOrDefault("BROWSE_IT_JDBC_PASSWORD", "");

        execute(serverUrl, "CREATE DATABASE " + SCHEMA + " CHARACTER SET utf8mb4");
        String schemaUrl = withDatabase(serverUrl, SCHEMA);
        try (Connection conn = DriverManager.getConnection(schemaUrl, user, password)) {
            runScript(conn, Path.of("src/main/resources/db/browse-schema.sql"));
            runScript(conn, Path.of("src/main/resources/db/browse-seed.sql"));
        }

        // DBUtil reads its config on first use
        System.setProperty("jdbc.url", schemaUrl);
        System.setProperty("jdbc.user", user);
        System.setProperty("jdbc.password", password);
    }

    @AfterAll
    static void dropSchema() throws Exception {
        if (serverUrl == null || serverUrl.isBlank()) {
            return;
        }
        if (System.getProperty("jdbc.url") != null) {
            DBUtil.shutdown();
        }
        execute(serverUrl, "DROP DATABASE IF EXISTS " + SCHEMA);
    }

    @Test
    void publishedProfilesComeBackInFeaturedOrderWithTheirCategory() {
        List<Profile> profiles = new JdbcBrowseProfileDao().findPublishedWithCategories();

        assertEquals(10, profiles.size());
        assertEquals("Marcus T.", profiles.get(0).getName());
        assertEquals("Sgt. Patricia H.", profiles.get(9).getName());
        assertEquals("Education", profiles.get(0).getFirstCategory().getLabel());
        assertEquals("Food & Nutrition", profiles.get(3).getFirstCategory().getLabel());
    }

    @Test
    void totalsAndCategoryStripAreComputedFromPublishedProfiles() {
        JdbcBrowseProfileDao dao = new JdbcBrowseProfileDao();

        assertEquals(new PublishedTotals(10, 21_830), dao.publishedTotals());
        List<CategoryRaised> strip = dao.raisedByCategory();
        assertEquals(List.of("Education", "Health", "Housing", "Food & Nutrition", "Veterans"),
                strip.stream().map(CategoryRaised::label).toList());
        assertEquals(List.of(1_700L, 9_300L, 3_000L, 1_130L, 6_700L),
                strip.stream().map(CategoryRaised::raised).toList());
    }

    @Test
    void topWeeklyIsTheFiveBiggestSevenDayTotals() {
        JdbcBrowseProfileDao dao = new JdbcBrowseProfileDao();
        Map<Long, Long> top = dao.topWeeklyRaised(5);

        Map<String, Long> byName = new HashMap<>();
        for (Profile p : dao.findPublishedWithCategories()) {
            if (top.containsKey(p.getId())) {
                byName.put(p.getName(), top.get(p.getId()));
            }
        }
        assertEquals(Map.of("Sgt. Patricia H.", 2_500L, "Diane & Ray K.", 1_800L, "Marcus T.", 1_400L,
                "Amara O.", 900L, "James W.", 750L), byName);
    }

    @Test
    void tickerShowsThisWeeksDonationsNewestFirst() {
        List<TickerItem> ticker = new JdbcDonationDao().recentTicker(12);

        assertEquals(12, ticker.size());
        assertEquals(new TickerItem("C. Washington", 2_500, "Sgt. Patricia H."), ticker.get(0));
        assertEquals(new TickerItem("Anonymous", 1_800, "Diane & Ray K."), ticker.get(1));
        assertEquals(new TickerItem("Anonymous", 150, "Sophia L."), ticker.get(11));
        assertEquals(4, ticker.stream().filter(t -> t.donorLabel().equals("Anonymous")).count());
    }

    @Test
    void donorStatsCountEachAnonymousGiftAsItsOwnDonor() {
        DonationDao.DonorStats stats = new JdbcDonationDao().donorStats();

        // 8 named donors + 4 anonymous gifts
        assertEquals(12, stats.activeDonors());
        assertEquals(9_300, stats.raisedThisWeek());
        // early on the 1st some demo donations are still in last month
        assertTrue(stats.newDonorsThisMonth() >= 0 && stats.newDonorsThisMonth() <= 12);
    }

    @Test
    void chipsListOnlyCategoriesInUseInCreationOrder() {
        assertEquals(List.of("Education", "Health", "Housing", "Food & Nutrition", "Veterans"),
                new JdbcCategoryDao().findInUse().stream().map(Category::getLabel).toList());
    }

    @Test
    void usersLoadWithDisplayNameAndAnOptionalPassword() {
        JdbcUserDao dao = new JdbcUserDao();

        User demo = dao.findByUsername("user").orElseThrow();
        assertEquals("Demo U.", demo.getDisplayName());
        assertNotNull(demo.getPasswordHash());
        assertNull(dao.findByUsername("cwashington").orElseThrow().getPasswordHash());
        assertTrue(dao.findByUsername("nobody").isEmpty());
    }

    private static void execute(String url, String sql) throws SQLException {
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    // jdbc:mysql://host:3306/?params -> jdbc:mysql://host:3306/<db>?params
    private static String withDatabase(String url, String database) {
        int query = url.indexOf('?');
        String base = query < 0 ? url : url.substring(0, query);
        String params = query < 0 ? "" : url.substring(query);
        return (base.endsWith("/") ? base : base + "/") + database + params;
    }

    // each statement ends with ";" at the end of a line
    private static void runScript(Connection conn, Path script) throws Exception {
        StringBuilder statement = new StringBuilder();
        try (Statement stmt = conn.createStatement()) {
            for (String line : Files.readAllLines(script)) {
                statement.append(line).append('\n');
                if (line.stripTrailing().endsWith(";")) {
                    String sql = statement.toString().strip();
                    statement.setLength(0);
                    stmt.execute(sql.substring(0, sql.length() - 1));
                }
            }
        }
    }
}
