package com.c3s.ai.admin.shell;

import com.c3s.ai.common.exception.EntityNotFoundException;
import com.c3s.ai.common.util.Log;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/** POST (field "id") - soft-delete one row, then back to its list with a confirmation. */
public abstract class ArchiveServlet extends AdminServlet {

    private static final Logger log = LoggerFactory.getLogger(ArchiveServlet.class);

    /** "Profile", "Category" - for the confirmation message. */
    protected abstract String entityLabel();

    protected abstract String listPath();

    protected abstract String notFoundMessage();

    /** Archive it and return its name/label. */
    protected abstract String archive(long id) throws EntityNotFoundException;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Long id = parseId(req.getParameter("id"));
        if (id == null) {
            log.warn("{} archive requested with malformed id: {}", entityLabel(), Log.clean(req.getParameter("id")));
            showNotFound(req, resp, notFoundMessage());
            return;
        }

        String name = loadOrRespond(req, resp, log, entityLabel() + " id=" + id, notFoundMessage(), () -> archive(id));
        if (name == null) {
            return;
        }
        log.info("Archived {} id={}", entityLabel(), id);
        writesDone(req);
        redirectWithFlash(req, resp, listPath(), FlashMessages.archived(entityLabel(), name));
    }
}
