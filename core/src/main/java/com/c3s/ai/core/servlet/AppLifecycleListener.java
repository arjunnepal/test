package com.c3s.ai.core.servlet;

import com.c3s.ai.common.config.Config;
import com.c3s.ai.common.db.DBUtil;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Startup: set the session cookie Secure flag from config and run one SELECT 1, so a bad DB
 * config shows up in the log at deploy time. The app starts either way. Shutdown: close the pool.
 */
@WebListener
public class AppLifecycleListener implements ServletContextListener {

    private static final Logger log = LoggerFactory.getLogger(AppLifecycleListener.class);
    private static final int PROBE_TIMEOUT_S = 5;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        boolean secure = Config.INSTANCE.getBoolean("session.cookie.secure", "SESSION_COOKIE_SECURE", true);
        try {
            sce.getServletContext().getSessionCookieConfig().setSecure(secure);
            if (secure) {
                log.info("Session cookie Secure flag = true");
            } else {
                log.warn("Session cookie Secure flag = false - only acceptable for local plain-HTTP development");
            }
        } catch (IllegalStateException e) {
            // some containers freeze SessionCookieConfig before this fires - fall back to web.xml
            log.warn("Could not set session cookie Secure flag programmatically ({}); "
                    + "set <secure> in web.xml if HTTPS-only", e.getMessage());
        }

        probeDatabase();
    }

    private void probeDatabase() {
        boolean ok;
        try {
            ok = DBUtil.ping(PROBE_TIMEOUT_S);
        } catch (Throwable t) {   // Throwable: a pool-init failure surfaces as ExceptionInInitializerError
            log.error("STARTUP DATABASE CHECK FAILED - the database pool could not be created.", t);
            return;
        }
        if (ok) {
            log.info("Startup database check OK");
        } else {
            log.error("STARTUP DATABASE CHECK FAILED - the app is up but cannot reach the database. "
                    + "Check jdbc.url / jdbc.user / jdbc.password and network access. /healthz will report DOWN.");
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
