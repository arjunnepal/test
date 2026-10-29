package com.c3s.ai.core.web;

/** Page object for login.jsp */
public class LoginPageData extends PageData {

    private String username;   // echoed back on a failed attempt
    private String next;       // the page to return to, already checked by the servlet
    private String action;     // the door the form posts back to - the staff page serves two

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNext() {
        return next;
    }

    public void setNext(String next) {
        this.next = next;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
