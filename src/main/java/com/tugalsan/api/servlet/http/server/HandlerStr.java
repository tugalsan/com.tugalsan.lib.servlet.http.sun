package com.tugalsan.api.servlet.http.server;

import com.tugalsan.api.pack.client.TGS_Pack2;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.servlet.http.HttpServlet;

public class HandlerStr extends HttpServlet {

    final public String slash_path;

    final private TGS_Pack2<String, HttpExchange> handle;

    private HandlerStr(String slash_path, <String, HttpExchange>       handle) {
        this.slash_path = slash_path;
        this.handle = handle;
    }

    public static HandlerStr of(String slash_path, <String, HttpExchange>       handle) {
        return new HandlerStr(slash_path, handle);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        var payload = handle.compile(exchange);
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, payload.length());
        try ( var responseBody = exchange.getResponseBody()) {
            responseBody.write(payload.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
