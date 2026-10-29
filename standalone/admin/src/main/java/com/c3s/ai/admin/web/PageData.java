package com.c3s.ai.admin.web;

// Base for the per-page object a servlet hands its JSP. Every page has its own
// subclass; the servlet stores it under the subclass's simple name and the JSP reads
// it back, e.g.  LoginPageData data = (LoginPageData) request.getAttribute("LoginPageData");
// The one shared field is 'message' - the flash / notice / error line, never null.
public abstract class PageData {

    private String message = "";

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message == null ? "" : message;
    }
}
