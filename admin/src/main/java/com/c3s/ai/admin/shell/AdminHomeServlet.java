package com.c3s.ai.admin.shell;

import com.c3s.ai.core.servlet.BaseServlet;
import com.c3s.ai.core.web.Routes;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** /admin -> /admin/profiles. The context root itself belongs to the giving page. */
@WebServlet(name = "AdminHomeServlet", urlPatterns = Routes.ADMIN_PREFIX)
public class AdminHomeServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        redirect(req, resp, Routes.ADMIN_HOME);
    }
}
