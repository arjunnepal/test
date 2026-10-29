package com.c3s.ai.browse.web;

public class LoginPageData extends PageData {

    // kept after a failed login
    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
