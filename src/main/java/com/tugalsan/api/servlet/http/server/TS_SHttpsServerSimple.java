package com.tugalsan.api.servlet.http.server;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import javax.net.ssl.SSLContext;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

public class TS_SHttpsServerSimple {

    public static void main(String[] args) throws Exception {
        var server = HttpsServer.create(new InetSocketAddress(8080), 0);
        var sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, null, null);
        server.setHttpsConfigurator(new HttpsConfigurator(sslContext));
        server.createContext("/", exchange -> {
            var response = "Hello World!";
            exchange.sendResponseHeaders(200, response.length());
            exchange.getResponseBody().write(response.getBytes());
        });
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();
    }
}
