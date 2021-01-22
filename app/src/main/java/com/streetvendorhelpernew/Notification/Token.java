package com.streetvendorhelpernew.Notification;

public class Token {

    private String token;
    private Boolean isServerToken;

    public Token() {
    }

    public Token(String token, Boolean isServerToken) {
        this.token = token;
        this.isServerToken = isServerToken;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public boolean isServerToken() {
        return isServerToken;
    }

    public void setServerToken(boolean serverToken) {
        isServerToken = serverToken;
    }

}
