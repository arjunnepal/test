package com.c3s.ai.admin.web;

// page object for login.jsp
public class LoginPageData extends PageData {

    private String username;   // echoed back on a failed attempt

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
