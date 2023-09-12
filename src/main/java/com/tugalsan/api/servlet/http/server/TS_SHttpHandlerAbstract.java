package com.tugalsan.api.servlet.http.server;

import com.sun.net.httpserver.*;
import com.tugalsan.api.callable.client.*;
import com.tugalsan.api.file.client.TGS_FileTypes;
import com.tugalsan.api.tuple.client.TGS_Tuple2;
import com.tugalsan.api.url.client.parser.TGS_UrlParser;
import com.tugalsan.api.validator.client.TGS_ValidatorType1;

public abstract class TS_SHttpHandlerAbstract implements HttpHandler {

    final public String slash_path;
    final protected TGS_CallableType1<TGS_Tuple2<TGS_FileTypes, String>, TS_SHttpHandlerRequest> request;
    final protected TGS_ValidatorType1<TGS_UrlParser> allow;

    protected TS_SHttpHandlerAbstract(String slash_path, TGS_ValidatorType1<TGS_UrlParser> allow, TGS_CallableType1<TGS_Tuple2<TGS_FileTypes, String>, TS_SHttpHandlerRequest> request) {
        this.slash_path = slash_path;
        this.allow = allow;
        this.request = request;
    }
}
