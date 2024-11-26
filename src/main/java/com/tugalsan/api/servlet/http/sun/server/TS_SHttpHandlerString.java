package com.tugalsan.api.servlet.http.sun.server;

import com.sun.net.httpserver.*;
import com.tugalsan.api.function.client.*;
import com.tugalsan.api.charset.client.TGS_CharSet;
import com.tugalsan.api.file.client.TGS_FileTypes;
import com.tugalsan.api.file.server.TS_FileUtils;
import com.tugalsan.api.file.server.TS_PathUtils;
import com.tugalsan.api.log.server.TS_Log;
import com.tugalsan.api.tuple.client.TGS_Tuple2;
import com.tugalsan.api.unsafe.client.TGS_UnSafe;
import com.tugalsan.api.url.client.TGS_Url;
import com.tugalsan.api.url.client.TGS_UrlUtils;
import com.tugalsan.api.url.client.parser.TGS_UrlParser;
import java.nio.charset.*;
import java.util.Objects;

public class TS_SHttpHandlerString extends TS_SHttpHandlerAbstract<String> {

    final private static TS_Log d = TS_Log.of(false, TS_SHttpHandlerString.class);

    private TS_SHttpHandlerString(String slash_path, TGS_Func_OutBool_In1<TS_SHttpHandlerRequest> allow, TGS_Func_OutTyped_In1<TGS_Tuple2<TGS_FileTypes, String>, TS_SHttpHandlerRequest> request, boolean removeHiddenChars) {
        super(slash_path, allow, request);
        this.removeHiddenChars = removeHiddenChars;
    }
    final public boolean removeHiddenChars;

    public static TS_SHttpHandlerString of(String slash_path, TGS_Func_OutBool_In1<TS_SHttpHandlerRequest> allow, TGS_Func_OutTyped_In1<TGS_Tuple2<TGS_FileTypes, String>, TS_SHttpHandlerRequest> request, boolean removeHiddenChars) {
        return new TS_SHttpHandlerString(slash_path, allow, request, removeHiddenChars);
    }

    @Override
    public void handle(HttpExchange httpExchange) {
        TGS_UnSafe.run(() -> {
            try (httpExchange) {
                d.ci("handle", "hello");
                //PARSER
                var uri = TS_SHttpUtils.getURI(httpExchange).orElse(null);
                if (uri == null) {
                    d.ce("handle", "ERROR sniff url from httpExchange is null ⚠");
                    TS_SHttpUtils.sendError404(httpExchange, "handle.string", "ERROR sniff url from httpExchange is null ⚠");
                    return;
                }
                var requestUrlString = removeHiddenChars ? TGS_CharSet.jre().makePrintable(uri.toString()) : uri.toString();
                var parser = TGS_UrlParser.of(TGS_Url.of(requestUrlString));
                if (d.infoEnable) {
                    d.ci("handle", "parser.toString", parser);
                    parser.quary.params.forEach(param -> {
                        d.ci("handle", "param", param);
                    });
                }
                if (TGS_UrlUtils.isHackedUrl(TGS_Url.of(parser.path.fileOrServletName))) {
                    TS_SHttpUtils.sendError404(httpExchange, "handle.string", "ERROR: hack detected ⚠ " + parser.path.toString_url());
                    return;
                }
                if (Objects.equals(parser.path.fileOrServletName, "favicon.ico")) {
//                    TS_SHttpUtils.sendError404(httpExchange, "handle.string", "INFO: favicon skipped " + parser.path.toString_url());
                    var pathFavicon = TS_PathUtils.getPathCurrent_nio(parser.path.fileOrServletName);
                    var bytesFavicon = TS_FileUtils.read(pathFavicon);
                    TGS_Tuple2<TGS_FileTypes, byte[]> payload = TGS_Tuple2.of(TGS_FileTypes.ico, bytesFavicon);
                    if (payload == null || payload.value0 == null || payload.value1 == null || payload.value1.length == 0) {
                        return;
                    }
                    {//SET HEADER
                        var headers = httpExchange.getResponseHeaders();
                        headers.add("Access-Control-Allow-Origin", "*");
                        headers.set("Content-Type", payload.value0.content);
                    }
                    {//SEND DATA
                        var data = payload.value1;
                        httpExchange.sendResponseHeaders(200, data.length);
                        try (var responseBody = httpExchange.getResponseBody()) {
                            responseBody.write(data);
                        }
                    }
                    return;
                }
                //GET PAYLOAD
                var requestBall = TS_SHttpHandlerRequest.of(httpExchange, parser);
                TGS_Tuple2<TGS_FileTypes, String> payload = allow.validate(requestBall)
                        ? request.call(requestBall)
                        : TGS_Tuple2.of(TGS_FileTypes.txt_utf8, "ERROR NOT_ALLOWED 👮");
                if (payload == null || payload.value0 == null || payload.value1 == null) {
                    return;
                }
                {//SET HEADER
                    var headers = httpExchange.getResponseHeaders();
                    headers.add("Access-Control-Allow-Origin", "*");
                    headers.set("Content-Type", payload.value0.content);
                }
                {//SEND DATA
                    var data = payload.value1.getBytes(StandardCharsets.UTF_8);
                    httpExchange.sendResponseHeaders(200, data.length);
                    try (var responseBody = httpExchange.getResponseBody()) {
                        responseBody.write(data);
                    }
                }
            }
        }, e -> d.ct("handle", e));
    }
}
