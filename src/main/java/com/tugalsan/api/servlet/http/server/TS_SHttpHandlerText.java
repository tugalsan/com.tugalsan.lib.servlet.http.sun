package com.tugalsan.api.servlet.http.server;

import com.sun.net.httpserver.*;
import com.tugalsan.api.callable.client.*;
import com.tugalsan.api.file.client.TGS_FileTypes;
import com.tugalsan.api.log.server.TS_Log;
import com.tugalsan.api.tuple.client.TGS_Tuple2;
import com.tugalsan.api.unsafe.client.TGS_UnSafe;
import com.tugalsan.api.url.client.TGS_Url;
import com.tugalsan.api.url.client.parser.TGS_UrlParser;
import com.tugalsan.api.validator.client.TGS_ValidatorType1;
import java.nio.charset.*;

public class TS_SHttpHandlerText extends TS_SHttpHandlerAbstract {

    final private static TS_Log d = TS_Log.of(true, TS_SHttpHandlerText.class);

    private TS_SHttpHandlerText(String slash_path, TGS_ValidatorType1<TGS_UrlParser> allow, TGS_CallableType1<TGS_Tuple2<TGS_FileTypes, String>, HttpExchange> httpExchange) {
        super(slash_path, allow, httpExchange);
    }

    public static TS_SHttpHandlerText of(String slash_path, TGS_ValidatorType1<TGS_UrlParser> allow, TGS_CallableType1<TGS_Tuple2<TGS_FileTypes, String>, HttpExchange> httpExchange) {
        return new TS_SHttpHandlerText(slash_path, allow, httpExchange);
    }

    @Override
    public void handle(HttpExchange httpExchange) {
        TGS_UnSafe.run(() -> {
            try (httpExchange) {
                //PARSER
                var uri = TS_SHttpUtils.getURI(httpExchange).orElse(null);
                if (uri == null) {
                    d.ce("handle.text", "ERROR sniff url from httpExchange is null");
                    TS_SHttpUtils.sendError404(httpExchange);
                    return;
                }
                var parser = TGS_UrlParser.of(TGS_Url.of(uri.toString()));
                if (d.infoEnable) {
                    d.ci("handle.text", "parser.toString", parser);
                    parser.quary.params.forEach(param -> {
                        d.ci("handle.text", "--param", param);
                    });
                }
                //GET PAYLOAD
                TGS_Tuple2<TGS_FileTypes, String> payload = allow.validate(parser)
                        ? this.httpExchange.call(httpExchange)
                        : TGS_Tuple2.of(TGS_FileTypes.txt_utf8, "ERROR NOT_ALLOWED");
                if (payload == null || payload.value0 == null|| payload.value1 == null) {
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
