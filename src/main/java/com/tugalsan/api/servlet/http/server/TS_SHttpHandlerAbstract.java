package com.tugalsan.api.servlet.http.server;

import com.sun.net.httpserver.*;
import com.tugalsan.api.callable.client.*;
import com.tugalsan.api.file.client.TGS_FileTypes;
import com.tugalsan.api.tuple.client.TGS_Tuple2;

public abstract class TS_SHttpHandlerAbstract implements HttpHandler {

    final public String slash_path;
    final protected TGS_CallableType1<TGS_Tuple2<TGS_FileTypes, String>, HttpExchange> handle;

    protected TS_SHttpHandlerAbstract(String slash_path, TGS_CallableType1<TGS_Tuple2<TGS_FileTypes, String>, HttpExchange> handle) {
        this.slash_path = slash_path;
        this.handle = handle;
    }
}
