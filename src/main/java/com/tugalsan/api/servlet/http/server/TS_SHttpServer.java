package com.tugalsan.api.servlet.http.server;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;
import javax.net.ssl.*;
import com.sun.net.httpserver.*;
import com.tugalsan.api.file.server.TS_DirectoryUtils;
import com.tugalsan.api.file.server.TS_FileUtils;
import com.tugalsan.api.log.server.TS_Log;
import com.tugalsan.api.unsafe.client.TGS_UnSafe;
import com.tugalsan.api.url.client.TGS_Url;
import com.tugalsan.api.url.client.parser.TGS_UrlParser;
import com.tugalsan.api.validator.client.TGS_ValidatorType1;

public class TS_SHttpServer {

    final private static TS_Log d = TS_Log.of(true, TS_SHttpServer.class);

    // The keystore is generated using the following three files:
    //    - private_key.key
    //    - site.crt
    //    - site.ca-bundle
    // ...and using the following set of commands (and password as "password"):
    //    openssl pkcs12 -export -out keystore.pkcs12 -inkey private_key.key -certfile site.ca-bundle -in site.crt
    //    keytool -v -importkeystore -srckeystore keystore.pkcs12 -srcstoretype PKCS12 -destkeystore keystore.jks -deststoretype pkcs12
    // initialise the keystore
    private static SSLContext createSSLContext(TS_SHttpConfigSSL ssl) {
        return TGS_UnSafe.call(() -> {
            //load keystore
            var ks = KeyStore.getInstance("PKCS12");
            try (var fis = new FileInputStream(ssl.p12.toAbsolutePath().toString())) {
                ks.load(fis, ssl.pass.toCharArray());
            }

            //convert keystore to key manager factories
            var kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, ssl.pass.toCharArray());
            var tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(ks);

            // create ssl Context
            var sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            return sslContext;
        }, e -> {
            d.ce("createSSLContext", e);
            return null;
        });
    }

    private static HttpServer createServer(TS_SHttpConfigNetwork network, SSLContext sslContext) {
        return TGS_UnSafe.call(() -> {
            if (sslContext == null) {
                return network.ip == null
                        ? HttpServer.create(new InetSocketAddress(network.port), 0)
                        : HttpServer.create(new InetSocketAddress(network.ip, network.port), 0);
            }
            var server = network.ip == null
                    ? HttpsServer.create(new InetSocketAddress(network.port), 0)//InetAddress.getLoopbackAddress() , 2
                    : HttpsServer.create(new InetSocketAddress(network.ip, network.port), 0);//, 2
            server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
                @Override
                public void configure(HttpsParameters params) {
                    TGS_UnSafe.run(() -> {
                        var newEngine = getSSLContext().createSSLEngine();
                        params.setNeedClientAuth(false);
                        params.setCipherSuites(newEngine.getEnabledCipherSuites());
                        params.setProtocols(newEngine.getEnabledProtocols());
                        params.setSSLParameters(getSSLContext().getSupportedSSLParameters());
                    }, e -> {
                        System.out.println("Failed to create HTTPS port");
                        System.out.println(e.getMessage());
                    });
                }
            });
            return server;
        }, e -> {
            d.ce("createServer", e);
            return null;
        });
    }

    private static void addHanders(HttpServer server, TS_SHttpHandlerAbstract... handlers) {
        Arrays.stream(handlers).forEach(handler -> server.createContext(handler.slash_path, handler));
    }

    private static void start(HttpServer server) {
        server.setExecutor(Executors.newCachedThreadPool(Thread.ofVirtual().factory()));
        server.start();
    }

    private static void sendError404(HttpExchange httpExchange) {
        TGS_UnSafe.run(() -> {
            try (httpExchange) {
                httpExchange.setAttribute("request-path", "ERROR Could not resolve request URI path " + httpExchange.getRequestURI());
                httpExchange.sendResponseHeaders(404, 0);
            }
        }, e -> e.printStackTrace());
    }

    private static void addHandlerFile(HttpServer server, Path fileHandlerRoot, TGS_ValidatorType1<TGS_UrlParser> url) {
        var fileHandler = SimpleFileServer.createFileHandler(fileHandlerRoot);
        d.ci("startHttpsServlet.fileHandler", "fileHandlerRoot", fileHandlerRoot);
        server.createContext("/file/", httpExchange -> {
            try (var a = httpExchange) {
                var uri = httpExchange.getRequestURI();
                var requestPath = uri.getPath();
                if (!TS_FileUtils.isExistFile(Path.of(requestPath))) {
                    sendError404(httpExchange);
                    return;
                }
                var parser = TGS_UrlParser.of(TGS_Url.of(uri.toString()));
                if (d.infoEnable) {
                    d.ci("startHttpsServlet.fileHandler", "parser.toString", parser);
                    parser.quary.params.forEach(param -> {
                        d.ci("startHttpsServlet.fileHandler", "--param", param);
                    });
                }
                if (!url.validate(parser)) {
                    return;
                }
                fileHandler.handle(httpExchange);
            }
        });
    }

    public static boolean startHttpsServlet(TS_SHttpConfigNetwork network, TS_SHttpConfigSSL ssl, TGS_ValidatorType1<TGS_UrlParser> allow, Path fileHandlerRoot, TS_SHttpHandlerAbstract... customHandlers) {
        return TGS_UnSafe.call(() -> {
            if (fileHandlerRoot != null && !TS_DirectoryUtils.isExistDirectory(fileHandlerRoot)) {
                d.ce("startHttpsServlet.fileHandler", "ERROR: fileHandlerRoot not exists", fileHandlerRoot);
                return false;
            }
            var sslContext = createSSLContext(ssl); //create ssl server
            if (sslContext == null) {
                d.ce("startHttpsServlet.fileHandler", "ERROR: createSSLContext returned null");
                return false;
            }
            var serverHttps = createServer(network, sslContext);
            if (serverHttps == null) {
                d.ce("startHttpsServlet.fileHandler", "ERROR: createServer returned null");
                return false;
            }
            if (fileHandlerRoot != null) {
                addHandlerFile(serverHttps, fileHandlerRoot, allow);
            }
            addHanders(serverHttps, customHandlers);
            start(serverHttps);
            d.ci("startHttpsServlet.fileHandler", "server started", network);
            if (!ssl.redirect) {
                return true;
            }
            var serverHttp = createServer(network.cloneIt().setPort(80), null);
            serverHttp.createContext("/", httpExchange -> {
                try (var a = httpExchange) {
                    var uri = TS_SHttpUtils.getURI(httpExchange).orElse(null);
                    if (uri == null) {
                        d.ce("handle", "ERROR url base null");
                        return;
                    }
                    var parser = TGS_UrlParser.of(TGS_Url.of(uri.toString()));
                    parser.protocol.value = "https://";
                    parser.host.port = network.port;
                    var redirectUrl = parser.toString();
                    d.ci("handle", "redirectUrl", redirectUrl);
                    httpExchange.getResponseHeaders().set("Location", redirectUrl);
                    TGS_UnSafe.run(() -> {
                        httpExchange.sendResponseHeaders(HttpURLConnection.HTTP_SEE_OTHER, -1);//responseLength = hasBody  ? 0 : -1
                    }, e -> d.ct("handle", e));
                }
            });
            start(serverHttp);
            return true;
        }, e -> {
            d.ce("startHttpsServlet", e);
            return false;
        });
    }
}
