package com.tugalsan.api.servlet.http.server;

import com.sun.net.httpserver.*;
import com.tugalsan.api.callable.client.*;

public abstract class TS_SHttpHandlerAbstract implements HttpHandler {

    final public String slash_path;
    final protected TGS_CallableType1<String, HttpExchange> handle;

    protected TS_SHttpHandlerAbstract(String slash_path, TGS_CallableType1<String, HttpExchange> handle) {
        this.slash_path = slash_path;
        this.handle = handle;
    }
}
