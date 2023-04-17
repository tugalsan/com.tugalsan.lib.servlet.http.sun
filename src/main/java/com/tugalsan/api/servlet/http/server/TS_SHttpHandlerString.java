package com.tugalsan.api.servlet.http.server;

import com.sun.net.httpserver.*;
import com.tugalsan.api.callable.client.*;
import com.tugalsan.api.unsafe.client.TGS_UnSafe;
import java.nio.charset.*;

public class TS_SHttpHandlerString extends TS_SHttpHandlerAbstract {

    private TS_SHttpHandlerString(String slash_path, TGS_CallableType1<String, HttpExchange> handle) {
        super(slash_path, handle);
    }

    public static TS_SHttpHandlerString of(String slash_path, TGS_CallableType1<String, HttpExchange> handle) {
        return new TS_SHttpHandlerString(slash_path, handle);
    }

    @Override
    public void handle(HttpExchange exchange) {
        TGS_UnSafe.run(() -> {
            var payload = handle.compile(exchange);
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, payload.length());
            try ( var responseBody = exchange.getResponseBody()) {
                responseBody.write(payload.getBytes(StandardCharsets.UTF_8));
            }
        }, e -> e.printStackTrace());
    }
}
