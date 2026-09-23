package com.c3s.ai.core.web;

/**
 * Base for the per-page object a servlet hands its JSP: stored under the class's simple name and
 * read back with request.getAttribute("LoginPageData"). The one shared field is the flash message.
 */
public class PageData {

    private String message = "";

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message == null ? "" : message;
    }

    /** The request attribute name this object is stored under. */
    public final String attributeName() {
        return getClass().getSimpleName();
    }
}
