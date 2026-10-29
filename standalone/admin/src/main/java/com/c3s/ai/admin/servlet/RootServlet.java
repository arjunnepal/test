package com.c3s.ai.admin.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

// Context root -> /profiles. Mapped to "" not "/" - "/" would make this the default
// servlet and swallow /css, /js etc. "" is only the root itself.
@WebServlet(name = "RootServlet", urlPatterns = "")
public class RootServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.sendRedirect(req.getContextPath() + "/profiles");
    }
}
