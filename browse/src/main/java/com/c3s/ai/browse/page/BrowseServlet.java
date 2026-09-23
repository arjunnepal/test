package com.c3s.ai.browse.page;

import com.c3s.ai.common.config.Config;
import com.c3s.ai.core.servlet.BaseServlet;
import com.c3s.ai.core.servlet.ErrorPages;
import com.c3s.ai.core.web.Routes;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;

/**
 * The giving page, reached at "/" through the index.jsp welcome file. Mapping it on "/" itself
 * would take the default servlet's place and swallow /css and /js.
 */
@WebServlet(name = "BrowseServlet", urlPatterns = Routes.BROWSE_PAGE)
public class BrowseServlet extends BaseServlet {

    private static final Logger log = LoggerFactory.getLogger(BrowseServlet.class);
    private static final String VIEW = "/WEB-INF/views/browse/browse.jsp";

    // Same data for every visitor, so it's cached (browse.cache.seconds, 0 = off). Dashboard
    // writes clear it, so an archived profile disappears from the site straight away.
    private BrowsePageCache pages;

    @Override
    public void init() {
        pages = BrowsePageCache.install(getServletContext(), new BrowseService()::buildPage,
                Duration.ofSeconds(Config.INSTANCE.getInt("browse.cache.seconds", "BROWSE_CACHE_SECONDS", 60)));
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            BrowsePageData data = pages.get();
            // a session means page chrome with a name and a sign-out token - keep it out of every cache
            if (req.getSession(false) != null) {
                noStore(resp);
            }
            render(req, resp, VIEW, data);
        } catch (RuntimeException e) {
            log.error("Failed to build the browse page", e);
            ErrorPages.serverError(req, resp);
        }
    }
}
