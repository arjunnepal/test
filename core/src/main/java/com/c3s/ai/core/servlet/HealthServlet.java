package com.c3s.ai.core.servlet;

import com.c3s.ai.common.db.DBUtil;
import com.c3s.ai.core.web.Routes;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * GET /healthz - SELECT 1, 200/503 plus a little JSON, unauthenticated. Capped at 4s so a
 * saturated pool can't make an orchestrator kill a box that's merely busy.
 */
@WebServlet(name = "HealthServlet", urlPatterns = Routes.HEALTH)
public class HealthServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(HealthServlet.class);
    private static final long TIMEOUT_MS = 4_000;
    private static final int QUERY_TIMEOUT_S = 3;

    // its own small pool so a stuck probe can't eat request threads
    private final ExecutorService probeExecutor = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "healthz-db-probe");
        t.setDaemon(true);
        return t;
    });

    @Override
    public void destroy() {
        probeExecutor.shutdownNow();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        boolean dbUp = checkDb();

        resp.setStatus(dbUp ? HttpServletResponse.SC_OK : HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        resp.setContentType("application/json;charset=UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        resp.getWriter().write("{\"status\":\"" + (dbUp ? "UP" : "DOWN")
                + "\",\"db\":\"" + (dbUp ? "UP" : "DOWN") + "\"}");
    }

    private boolean checkDb() {
        Future<Boolean> probe = probeExecutor.submit(HealthServlet::probe);
        try {
            return probe.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            probe.cancel(true);
            log.warn("Health check DB probe timed out after {} ms", TIMEOUT_MS);
            return false;
        } catch (ExecutionException e) {
            log.warn("Health check DB probe errored", e.getCause());
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static boolean probe() {
        try {
            return DBUtil.ping(QUERY_TIMEOUT_S);
        } catch (RuntimeException | Error e) {   // a pool-init failure surfaces as ExceptionInInitializerError
            log.warn("Health check DB probe failed: {}", e.toString());
            return false;
        }
    }
}
