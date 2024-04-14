package com.tugalsan.api.servlet.http.server;

import com.sun.net.httpserver.*;
import com.tugalsan.api.callable.client.*;
import com.tugalsan.api.coronator.client.TGS_Coronator;
import com.tugalsan.api.file.client.TGS_FileTypes;
import com.tugalsan.api.log.server.TS_Log;
import com.tugalsan.api.string.client.TGS_StringUtils;
import com.tugalsan.api.validator.client.TGS_ValidatorType1;

public abstract class TS_SHttpHandlerAbstract<T> implements HttpHandler {

    public static record Item<T>(TGS_FileTypes type, T data) {

    }
    final private static TS_Log d = TS_Log.of(false, TS_SHttpHandlerAbstract.class);

    final public String slash_path;
    final protected TGS_CallableType1<Item<T>, TS_SHttpHandlerRequest> request;
    final protected TGS_ValidatorType1<TS_SHttpHandlerRequest> allow;

    protected TS_SHttpHandlerAbstract(String slash_path, TGS_ValidatorType1<TS_SHttpHandlerRequest> allow, TGS_CallableType1<Item<T>, TS_SHttpHandlerRequest> request) {
        this.slash_path = TGS_Coronator.ofStr()
                .anoint(val -> slash_path)
                .anointIf(TGS_StringUtils::isNullOrEmpty, val -> {
                    d.ci("constructor", "TGS_StringUtils::isNullOrEmpty", "set as '/'");
                    return "/";
                })
                .anointIf(val -> val.charAt(0) != '/', val -> {
                    d.ci("constructor", "val.charAt(0) != '/'", "add '/' to the left");
                    return "/" + val;
                })
                .coronate();
        d.ci("constructor", "slash_path", slash_path);
        this.allow = allow;
        this.request = request;
    }
}
