package com.tugalsan.api.servlet.http.server;

import java.nio.file.Path;

public class TS_SHttpConfigSSL {

    private TS_SHttpConfigSSL(Path p12, String pass, boolean redirect) {
        this.p12 = p12;
        this.pass = pass;
        this.redirect = redirect;
    }
    public Path p12;
    public String pass;
    public boolean redirect;

    public static TS_SHttpConfigSSL of(Path p12, String pass, boolean redirect) {
        return new TS_SHttpConfigSSL(p12, pass, redirect);
    }

    @Override
    public String toString() {
        return TS_SHttpConfigSSL.class.getSimpleName() + "{" + "p12=" + p12 + ", pass=" + pass + ", redirect=" + redirect + '}';
    }

}
