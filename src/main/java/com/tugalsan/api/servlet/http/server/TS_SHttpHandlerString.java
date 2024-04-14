package com.tugalsan.api.servlet.http.server;

import com.sun.net.httpserver.*;
import com.tugalsan.api.callable.client.*;
import com.tugalsan.api.charset.server.TS_CharSetUtils;
import com.tugalsan.api.file.client.TGS_FileTypes;
import com.tugalsan.api.file.server.TS_FileUtils;
import com.tugalsan.api.file.server.TS_PathUtils;
import com.tugalsan.api.log.server.TS_Log;
import com.tugalsan.api.url.client.TGS_Url;
import com.tugalsan.api.url.client.TGS_UrlUtils;
import com.tugalsan.api.url.client.parser.TGS_UrlParser;
import com.tugalsan.api.validator.client.TGS_ValidatorType1;
import java.io.IOException;
import java.nio.charset.*;
import java.util.Objects;

public class TS_SHttpHandlerString extends TS_SHttpHandlerAbstract<String> {

    final private static TS_Log d = TS_Log.of(false, TS_SHttpHandlerString.class);

    private TS_SHttpHandlerString(String slash_path, TGS_ValidatorType1<TS_SHttpHandlerRequest> allow, TGS_CallableType1<Item<String>, TS_SHttpHandlerRequest> request, boolean removeHiddenChars) {
        super(slash_path, allow, request);
        this.removeHiddenChars = removeHiddenChars;
    }
    final public boolean removeHiddenChars;

    public static TS_SHttpHandlerString of(String slash_path, TGS_ValidatorType1<TS_SHttpHandlerRequest> allow, TGS_CallableType1<Item<String>, TS_SHttpHandlerRequest> request, boolean removeHiddenChars) {
        return new TS_SHttpHandlerString(slash_path, allow, request, removeHiddenChars);
    }

    @Override
    public void handle(HttpExchange httpExchange) {
        try (httpExchange) {
            d.ci("handle", "hello");
            //PARSER
            var uri = TS_SHttpUtils.getURI(httpExchange).orElse(null);
            if (uri == null) {
                d.ce("handle", "ERROR sniff url from httpExchange is null ⚠");
                TS_SHttpUtils.sendError404(httpExchange, "handle.string", "ERROR sniff url from httpExchange is null ⚠");
                return;
            }
            var requestUrlString = removeHiddenChars ? TS_CharSetUtils.makePrintable(uri.toString()) : uri.toString();
            var u_parser = TGS_UrlParser.of(TGS_Url.of(requestUrlString));
            if (u_parser.isExcuse()) {
                d.ct("handle", u_parser.excuse());
                return;
            }
            var parser = u_parser.value();
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
                var u_bytesFavicon = TS_FileUtils.read(pathFavicon);
                if (u_bytesFavicon.isExcuse()) {
                    d.ce("handle", u_bytesFavicon.excuse());
                    return;
                }
                Item<byte[]> payload = new Item(TGS_FileTypes.ico, u_bytesFavicon.value());
                if (payload.type() == null || payload.data() == null || payload.data().length == 0) {
                    d.ce("handle", "payload.type() == null || payload.data() == null || payload.data().length == 0");
                    return;
                }
                {//SET HEADER
                    var headers = httpExchange.getResponseHeaders();
                    headers.add("Access-Control-Allow-Origin", "*");
                    headers.set("Content-Type", payload.type().content);
                }
                {//SEND DATA
                    var data = payload.data();
                    httpExchange.sendResponseHeaders(200, data.length);
                    try (var responseBody = httpExchange.getResponseBody()) {
                        responseBody.write(data);
                    }
                }
                return;
            }
            //GET PAYLOAD
            var requestBall = TS_SHttpHandlerRequest.of(httpExchange, parser);

            Item<String> payload;
            if (allow.validate(requestBall)) {
                payload = request.call(requestBall);
            } else {
                payload = new Item(TGS_FileTypes.txt_utf8, "ERROR NOT_ALLOWED 👮");
            }

            if (payload == null || payload.type() == null || payload.data() == null) {
                d.ce("handle", "payload == null || payload.type() == null || payload.data() == null");
                return;
            }
            {//SET HEADER
                var headers = httpExchange.getResponseHeaders();
                headers.add("Access-Control-Allow-Origin", "*");
                headers.set("Content-Type", payload.type().content);
            }
            {//SEND DATA
                var data = payload.data().getBytes(StandardCharsets.UTF_8);
                httpExchange.sendResponseHeaders(200, data.length);
                try (var responseBody = httpExchange.getResponseBody()) {
                    responseBody.write(data);
                }
            }
        } catch (IOException ex) {
            d.ct("handle", ex);
        }
    }
}
