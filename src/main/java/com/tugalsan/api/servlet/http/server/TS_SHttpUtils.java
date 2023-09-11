package com.tugalsan.api.servlet.http.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpsServer;
import com.tugalsan.api.coronator.client.TGS_Coronator;
import com.tugalsan.api.unsafe.client.TGS_UnSafe;
import java.net.URI;
import java.util.Optional;

public class TS_SHttpUtils {

    public static Optional<URI> getURI(HttpExchange ex) {
        var host = TGS_Coronator.ofStr()
                .anoint(val -> ex.getRequestHeaders().getFirst("Host"))
                .anointIf(val -> val == null, val -> "localhost:" + ex.getHttpContext().getServer().getAddress().getPort())
                .coronate();
        var protocol = (ex.getHttpContext().getServer() instanceof HttpsServer) ? "https" : "http";
        var base = TGS_UnSafe.call(() -> new URI(protocol, host, "/", null, null), e -> null);
        if (base == null) {
            return Optional.empty();
        }
        var requestedUri = ex.getRequestURI();
        return Optional.of(base.resolve(requestedUri));
    }

    public static void sendError404(HttpExchange httpExchange) {
        TGS_UnSafe.run(() -> {
            try (httpExchange) {
                httpExchange.setAttribute("request-path", "ERROR Could not resolve request URI path " + httpExchange.getRequestURI());
                httpExchange.sendResponseHeaders(404, 0);
            }
        }, e -> e.printStackTrace());
    }
}
