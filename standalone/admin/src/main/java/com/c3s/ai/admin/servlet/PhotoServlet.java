package com.c3s.ai.admin.servlet;

import com.c3s.ai.admin.upload.UploadUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

// GET /photos/{filename} - streams a stored photo, 404 if it's missing or not a name we'd generate
@WebServlet(name = "PhotoServlet", urlPatterns = "/photos/*")
public class PhotoServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(PhotoServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        String filename = pathInfo == null ? null : pathInfo.replaceFirst("^/", "");

        // must match the <uuid>.<ext> pattern - blocks traversal, never hits the FS with raw input
        if (!UploadUtil.isSafeFilename(filename)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Path file = UploadUtil.directory().resolve(filename);
        if (!Files.isRegularFile(file)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try {
            resp.setContentType(UploadUtil.contentType(filename));
            resp.setContentLengthLong(Files.size(file));
            resp.setHeader("X-Content-Type-Options", "nosniff");
            // name is a stable UUID, but it's behind auth - private, not shared caches
            resp.setHeader("Cache-Control", "private, max-age=31536000, immutable");
            Files.copy(file, resp.getOutputStream());
        } catch (IOException e) {
            // the response is already streaming; a failure here is almost always the client
            // hanging up mid-download, which isn't a server error worth a stack trace
            log.debug("Photo stream to client did not finish for {}: {}", filename, e.toString());
            if (!resp.isCommitted()) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }
}
