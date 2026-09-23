package com.c3s.ai.admin.shell;

import com.c3s.ai.core.web.AppCaches;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/** Registers the dashboard's own cache (sidebar stats) for clearing on every write. */
@WebListener
public class AdminCachesListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        AppCaches.register(sce.getServletContext(), SidebarStatsCache.INSTANCE::invalidate);
    }
}
