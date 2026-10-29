package com.c3s.ai.browse.web;

public abstract class PageData {

    private String message = "";

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message == null ? "" : message;
    }
}
