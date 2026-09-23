package com.c3s.ai.admin.profile;

import com.c3s.ai.admin.shell.ArchiveServlet;
import com.c3s.ai.common.exception.EntityNotFoundException;
import com.c3s.ai.core.web.Routes;
import jakarta.servlet.annotation.WebServlet;

/** POST /admin/profiles/archive (field "id") - soft-delete a profile, back to the list */
@WebServlet(name = "ProfileArchiveServlet", urlPatterns = Routes.ADMIN_PROFILES_ARCHIVE)
public class ProfileArchiveServlet extends ArchiveServlet {

    private final ProfileService service = new ProfileService();

    @Override
    protected String entityLabel() {
        return "Profile";
    }

    @Override
    protected String listPath() {
        return Routes.ADMIN_PROFILES;
    }

    @Override
    protected String notFoundMessage() {
        return ProfileServlet.NOT_FOUND_MESSAGE;
    }

    @Override
    protected String archive(long id) throws EntityNotFoundException {
        return service.archive(id);
    }
}
