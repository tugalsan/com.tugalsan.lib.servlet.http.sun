package com.tugalsan.api.servlet.http.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpsServer;
import com.tugalsan.api.coronator.client.TGS_Coronator;
import com.tugalsan.api.log.server.TS_Log;
import com.tugalsan.api.network.client.TGS_NetworkIPUtils;
import com.tugalsan.api.unsafe.client.TGS_UnSafe;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;

public class TS_SHttpUtils {

    final private static TS_Log d = TS_Log.of(false, TS_SHttpUtils.class);

    public static boolean isLocalHost(HttpExchange httpExchange) {
        return Objects.equals(
                httpExchange.getLocalAddress().getHostName(),
                httpExchange.getRemoteAddress().getHostName()
        );
    }

    public static boolean isLocalClient(HttpExchange httpExchange) {
        var clientIp = httpExchange.getRemoteAddress().getHostName();
        return isLocalHost(httpExchange)
                || TGS_NetworkIPUtils.isLocalClient("192.168.5.14", clientIp)
                || TGS_NetworkIPUtils.isLocalClient("192.168.7.1", clientIp)
                || TGS_NetworkIPUtils.isLocalClient("10.0.0.10", clientIp);
    }

    public static Optional<URI> getURI(HttpExchange httpExchange) {
        var host = TGS_Coronator.ofStr()
                .anoint(val -> httpExchange.getRequestHeaders().getFirst("Host"))
                .anointIf(val -> val == null, val -> "localhost:" + httpExchange.getHttpContext().getServer().getAddress().getPort())
                .coronate();
        var protocol = (httpExchange.getHttpContext().getServer() instanceof HttpsServer) ? "https" : "http";
        var base = TGS_UnSafe.call(() -> new URI(protocol, host, "/", null, null), e -> null);
        if (base == null) {
            return Optional.empty();
        }
        var requestedUri = httpExchange.getRequestURI();
        return Optional.of(base.resolve(requestedUri));
    }

    public static void sendError404(HttpExchange httpExchange, CharSequence funcName, CharSequence consoleErrorMessage) {
        TGS_UnSafe.run(() -> {
            try (httpExchange) {
                d.ce("sendError404", funcName, consoleErrorMessage);
                httpExchange.setAttribute("request-path", "ERROR Could not resolve request URI path " + httpExchange.getRequestURI());
                httpExchange.sendResponseHeaders(404, 0);
            }
        }, e -> e.printStackTrace());
    }
}
