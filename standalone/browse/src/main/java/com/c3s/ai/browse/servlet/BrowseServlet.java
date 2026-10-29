package com.c3s.ai.browse.servlet;

import com.c3s.ai.browse.service.BrowsePageCache;
import com.c3s.ai.browse.service.BrowseService;
import com.c3s.ai.browse.util.Config;
import com.c3s.ai.browse.web.BrowsePageData;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;

// "" is just the context root; "/" would also swallow /css and /js
@WebServlet(name = "BrowseServlet", urlPatterns = "")
public class BrowseServlet extends PageServlet {

    private static final Logger log = LoggerFactory.getLogger(BrowseServlet.class);
    private static final String VIEW = "/WEB-INF/views/browse.jsp";

    // same data for every visitor, so it's cached (browse.cache.seconds, 0 = off)
    private final BrowsePageCache pages = new BrowsePageCache(new BrowseService()::buildPage,
            Duration.ofSeconds(Config.INSTANCE.getInt("browse.cache.seconds", "BROWSE_CACHE_SECONDS", 60)));

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            BrowsePageData data = pages.get();
            // signed-in pages shouldn't end up in a shared cache
            if (req.getSession(false) != null) {
                resp.setHeader("Cache-Control", "private, no-cache");
            }
            dispatch(req, resp, VIEW, HttpServletResponse.SC_OK, data);
        } catch (RuntimeException e) {
            log.error("Failed to build the browse page", e);
            showServerError(req, resp);
        }
    }
}
