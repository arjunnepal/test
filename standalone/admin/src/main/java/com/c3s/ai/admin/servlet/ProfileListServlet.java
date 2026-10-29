package com.c3s.ai.admin.servlet;

import com.c3s.ai.admin.model.Profile;
import com.c3s.ai.admin.service.ProfileService;
import com.c3s.ai.admin.web.ProfileListPageData;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

// GET /profiles - every profile, any status. The app's home page.
@WebServlet(name = "ProfileListServlet", urlPatterns = "/profiles")
public class ProfileListServlet extends AdminServlet {

    private static final Logger log = LoggerFactory.getLogger(ProfileListServlet.class);
    private static final String LIST_VIEW = "/WEB-INF/views/profiles-list.jsp";

    private final ProfileService service = new ProfileService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        List<Profile> profiles = loadListOrRespond(req, resp, log, "profile list", service::listAll);
        if (profiles == null) {
            return;
        }
        ProfileListPageData data = new ProfileListPageData();
        data.setProfiles(profiles);
        render(req, resp, LIST_VIEW, data);
    }
}
