package com.c3s.ai.admin.profile;

import com.c3s.ai.admin.shell.AdminServlet;
import com.c3s.ai.core.web.Routes;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/** GET /admin/profiles - every profile, any status. The dashboard's home page. */
@WebServlet(name = "ProfileListServlet", urlPatterns = Routes.ADMIN_PROFILES)
public class ProfileListServlet extends AdminServlet {

    private static final Logger log = LoggerFactory.getLogger(ProfileListServlet.class);
    private static final String VIEW = "/WEB-INF/views/admin/profiles-list.jsp";

    private final ProfileService service = new ProfileService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        List<ProfileListItem> profiles = loadListOrRespond(req, resp, log, "profile list", service::listAll);
        if (profiles != null) {
            show(req, resp, VIEW, new ProfileListPageData(profiles));
        }
    }
}
