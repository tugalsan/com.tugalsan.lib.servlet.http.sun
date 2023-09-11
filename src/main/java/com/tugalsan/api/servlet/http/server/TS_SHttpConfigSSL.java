package com.tugalsan.api.servlet.http.server;

import java.nio.file.Path;

public class TS_SHttpConfigSSL {

    private TS_SHttpConfigSSL(Path p12, String pass, boolean redirectToSSL) {
        this.p12 = p12;
        this.pass = pass;
        this.redirectToSSL = redirectToSSL;
    }
    public Path p12;
    public String pass;
    public boolean redirectToSSL;

    public static TS_SHttpConfigSSL of(Path p12, String pass, boolean redirectToSSL) {
        return new TS_SHttpConfigSSL(p12, pass, redirectToSSL);
    }

    @Override
    public String toString() {
        return TS_SHttpConfigSSL.class.getSimpleName() + "{" + "p12=" + p12 + ", pass=" + pass + ", redirect=" + redirectToSSL + '}';
    }

}
