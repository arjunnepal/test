package com.c3s.ai.admin.servlet;

import com.c3s.ai.admin.service.ProfileService;
import com.c3s.ai.admin.util.FlashMessages;
import com.c3s.ai.admin.util.Log;
import com.c3s.ai.admin.web.FlashScope;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

// POST /profiles/archive (field "id") - soft-delete a profile, back to the list
@WebServlet(name = "ProfileArchiveServlet", urlPatterns = "/profiles/archive")
public class ProfileArchiveServlet extends AdminServlet {

    private static final Logger log = LoggerFactory.getLogger(ProfileArchiveServlet.class);
    private static final String NOT_FOUND_MESSAGE = "That profile could not be found.";

    private final ProfileService service = new ProfileService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Long id = parseId(req.getParameter("id"));
        if (id == null) {
            log.warn("Archive requested with malformed id: {}", Log.clean(req.getParameter("id")));
            showNotFound(req, resp, NOT_FOUND_MESSAGE);
            return;
        }

        String name = loadOrRespond(req, resp, log, "profile id=" + id, NOT_FOUND_MESSAGE,
                () -> service.archive(id));
        if (name == null) {
            return;
        }
        log.info("Archived profile id={}", id);

        FlashScope.push(req, FlashMessages.archived("Profile", name));
        resp.sendRedirect(req.getContextPath() + "/profiles");
    }
}
