package com.tugalsan.api.servlet.http.server;

import com.sun.net.httpserver.HttpExchange;
import com.tugalsan.api.url.client.parser.TGS_UrlParser;

public class TS_SHttpHandlerRequest {

    private TS_SHttpHandlerRequest(HttpExchange httpExchange, TGS_UrlParser url) {
        this.httpExchange = httpExchange;
        this.url = url;
    }
    final public HttpExchange httpExchange;
    final public TGS_UrlParser url;

    public static TS_SHttpHandlerRequest of(HttpExchange httpExchange, TGS_UrlParser url) {
        return new TS_SHttpHandlerRequest(httpExchange, url);
    }

    final public boolean isLocalHost() {
        return TS_SHttpUtils.isLocalHost(httpExchange);
    }

    final public boolean isLocalClient() {
        return TS_SHttpUtils.isLocalClient(httpExchange);
    }

    final public void sendError404(CharSequence funcName, CharSequence consoleErrorMessage) {
        TS_SHttpUtils.sendError404(httpExchange, funcName, consoleErrorMessage);
    }
}
