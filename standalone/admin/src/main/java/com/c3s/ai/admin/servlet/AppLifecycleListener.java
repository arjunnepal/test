package com.c3s.ai.admin.servlet;

import com.c3s.ai.admin.db.DBUtil;
import com.c3s.ai.admin.util.Config;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.Statement;

/**
 * Startup: set the session cookie Secure flag from config (here, not web.xml, so plain-HTTP
 * dev still works) and run one SELECT 1 so a bad DB config shows up in the log at deploy
 * time. The app starts either way - /healthz reports the live DB state. Shutdown: close the pool.
 */
@WebListener
public class AppLifecycleListener implements ServletContextListener {

    private static final Logger log = LoggerFactory.getLogger(AppLifecycleListener.class);
    private static final int PROBE_TIMEOUT_S = 5;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        boolean secure = Config.INSTANCE.getBoolean("session.cookie.secure", "SESSION_COOKIE_SECURE", false);
        try {
            sce.getServletContext().getSessionCookieConfig().setSecure(secure);
            log.info("Session cookie Secure flag = {}", secure);
        } catch (IllegalStateException e) {
            // some containers freeze SessionCookieConfig before this fires - fall back to web.xml
            log.warn("Could not set session cookie Secure flag programmatically ({}); "
                    + "set <secure> in web.xml if HTTPS-only", e.getMessage());
        }

        probeDatabase();
    }

    private void probeDatabase() {
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.setQueryTimeout(PROBE_TIMEOUT_S);
            stmt.execute("SELECT 1");
            log.info("Startup database check OK");
        } catch (Throwable t) {   // Throwable: a pool-init failure surfaces as ExceptionInInitializerError
            log.error("STARTUP DATABASE CHECK FAILED - the app is up but cannot reach the database. "
                    + "Check jdbc.url / jdbc.user / jdbc.password and network access. /healthz will report DOWN.", t);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        try {
            DBUtil.shutdown();
        } catch (Throwable t) {
            log.warn("DB pool shutdown skipped/failed", t);
        }
    }
}
