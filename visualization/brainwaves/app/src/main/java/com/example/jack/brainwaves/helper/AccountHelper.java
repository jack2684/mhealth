package com.example.jack.brainwaves.helper;

/**
 * Created by jack on 3/4/15.
 */
public class AccountHelper {
    protected String username;
    protected boolean login;

    public void setLogin(String username) {
        if(username != null || !username.equals("")) {
            this.username = username;
            this.login = true;
        }
    }

    public void setLogout() {
        this.login = false;
    }

    public boolean isLogin() {
        return this.login;
    }

    public String getUsername() {
        return this.username;
    }
}
