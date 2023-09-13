package com.tugalsan.api.servlet.http.server;

import com.sun.net.httpserver.*;
import com.tugalsan.api.callable.client.*;
import com.tugalsan.api.charset.server.TS_CharSetUtils;
import com.tugalsan.api.file.client.TGS_FileTypes;
import com.tugalsan.api.log.server.TS_Log;
import com.tugalsan.api.tuple.client.TGS_Tuple2;
import com.tugalsan.api.unsafe.client.TGS_UnSafe;
import com.tugalsan.api.url.client.TGS_Url;
import com.tugalsan.api.url.client.TGS_UrlUtils;
import com.tugalsan.api.url.client.parser.TGS_UrlParser;
import com.tugalsan.api.validator.client.TGS_ValidatorType1;
import java.nio.charset.*;

public class TS_SHttpHandlerString extends TS_SHttpHandlerAbstract {

    final private static TS_Log d = TS_Log.of(true, TS_SHttpHandlerString.class);

    private TS_SHttpHandlerString(String slash_path, TGS_ValidatorType1<TS_SHttpHandlerRequest> allow, TGS_CallableType1<TGS_Tuple2<TGS_FileTypes, String>, TS_SHttpHandlerRequest> request) {
        super(slash_path, allow, request);
    }

    public static TS_SHttpHandlerString of(String slash_path, TGS_ValidatorType1<TS_SHttpHandlerRequest> allow, TGS_CallableType1<TGS_Tuple2<TGS_FileTypes, String>, TS_SHttpHandlerRequest> request) {
        return new TS_SHttpHandlerString(slash_path, allow, request);
    }

    @Override
    public void handle(HttpExchange httpExchange) {
        TGS_UnSafe.run(() -> {
            try (httpExchange) {
                //PARSER
                var uri = TS_SHttpUtils.getURI(httpExchange).orElse(null);
                if (uri == null) {
                    d.ce("handle", "ERROR sniff url from httpExchange is null ⚠");
                    TS_SHttpUtils.sendError404(httpExchange);
                    return;
                }
                var parser = TGS_UrlParser.of(TGS_Url.of(TS_CharSetUtils.makePrintable(uri.toString())));
                if (TGS_UrlUtils.isHackedUrl(TGS_Url.of(parser.path.fileOrServletName))) {
                    d.ce("handle", "ERROR: hack detected ⚠", parser.path.toString_url());
                    TS_SHttpUtils.sendError404(httpExchange);
                    return;
                }
                if (d.infoEnable) {
                    d.ci("handle", "parser.toString", parser);
                    parser.quary.params.forEach(param -> {
                        d.ci("handle", "param", param);
                    });
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
        }, e -> e.printStackTrace());
    }
}
