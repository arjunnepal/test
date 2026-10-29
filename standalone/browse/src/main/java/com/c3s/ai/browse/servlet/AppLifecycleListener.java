package com.c3s.ai.browse.servlet;

import com.c3s.ai.browse.db.DBUtil;
import com.c3s.ai.browse.util.Config;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.Statement;

// Sets the session cookie Secure flag and checks the DB on startup, closes the pool on shutdown.
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
            // some containers don't allow this here, use web.xml then
            log.warn("Could not set the session cookie Secure flag ({}); "
                    + "add <secure>true</secure> to web.xml if the site is HTTPS-only", e.getMessage());
        }

        probeDatabase();
    }

    private void probeDatabase() {
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.setQueryTimeout(PROBE_TIMEOUT_S);
            stmt.execute("SELECT 1");
            log.info("Startup database check OK");
        } catch (Throwable t) {
            // pool setup errors arrive as ExceptionInInitializerError
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
