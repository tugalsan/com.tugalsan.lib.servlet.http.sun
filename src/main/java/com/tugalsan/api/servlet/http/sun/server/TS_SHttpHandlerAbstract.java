package com.tugalsan.api.servlet.http.sun.server;

import com.tugalsan.api.function.client.maythrow.uncheckedexceptions.TGS_FuncMTUCE_OutBool_In1;
import com.tugalsan.api.function.client.maythrow.uncheckedexceptions.TGS_FuncMTUCE_OutTyped_In1;
import com.sun.net.httpserver.*;
import com.tugalsan.api.file.client.TGS_FileTypes;
import com.tugalsan.api.function.client.maythrow.uncheckedexceptions.TGS_FuncMTUCEEffectivelyFinal;
import com.tugalsan.api.log.server.TS_Log;
import com.tugalsan.api.string.client.TGS_StringUtils;
import com.tugalsan.api.tuple.client.TGS_Tuple2;

public abstract class TS_SHttpHandlerAbstract<T> implements HttpHandler {

    final private static TS_Log d = TS_Log.of(false, TS_SHttpHandlerAbstract.class);

    final public String slash_path;
    final protected TGS_FuncMTUCE_OutTyped_In1<TGS_Tuple2<TGS_FileTypes, T>, TS_SHttpHandlerRequest> request;
    final protected TGS_FuncMTUCE_OutBool_In1<TS_SHttpHandlerRequest> allow;

    protected TS_SHttpHandlerAbstract(String slash_path, TGS_FuncMTUCE_OutBool_In1<TS_SHttpHandlerRequest> allow, TGS_FuncMTUCE_OutTyped_In1<TGS_Tuple2<TGS_FileTypes, T>, TS_SHttpHandlerRequest> request) {
        this.slash_path = TGS_FuncMTUCEEffectivelyFinal.ofStr()
                .anoint(val -> slash_path)
                .anointIf(TGS_StringUtils.cmn()::isNullOrEmpty, val -> {
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
