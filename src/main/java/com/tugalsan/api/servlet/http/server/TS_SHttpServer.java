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
import com.tugalsan.api.url.client.TGS_UrlUtils;
import com.tugalsan.api.url.client.parser.TGS_UrlParser;

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

    private static void addHanders(HttpServer httpServer, TS_SHttpHandlerAbstract... handlers) {
        Arrays.stream(handlers).forEach(handler -> httpServer.createContext(handler.slash_path, handler));
    }

    private static void start(HttpServer httpServer) {
        httpServer.setExecutor(Executors.newCachedThreadPool(Thread.ofVirtual().factory()));
        httpServer.start();
    }

    private static void addHandlerFile(HttpServer httpServer, TS_SHttpConfigHandlerFile fileHandlerConfig) {
        var fileHandler = SimpleFileServer.createFileHandler(fileHandlerConfig.root);
        d.ci("addHandlerFile", "fileHandlerConfig.root", fileHandlerConfig.root);
        httpServer.createContext(fileHandlerConfig.slash_path_slash, httpExchange -> {
            try (httpExchange) {
                d.ci("addHandlerFile", "hello");
                var uri = TS_SHttpUtils.getURI(httpExchange).orElse(null);
                if (uri == null) {
                    d.ce("addHandlerFile", "ERROR sniff url from httpExchange is null");
                    TS_SHttpUtils.sendError404(httpExchange);
                    return;
                }
                var requestPath = uri.getPath();
                if (!TS_FileUtils.isExistFile(Path.of(requestPath))) {
                    TS_SHttpUtils.sendError404(httpExchange);
                    return;
                }
                var parser = TGS_UrlParser.of(TGS_Url.of(uri.toString()));
                if (TGS_UrlUtils.isHackedUrl(TGS_Url.of(parser.path.fileOrServletName))) {
                    d.ce("addHandlerFile", "ERROR: hack detected ⚠", parser.path.toString_url());
                    TS_SHttpUtils.sendError404(httpExchange);
                    return;
                }
                if (d.infoEnable) {
                    d.ci("addHandlerFile", "parser.toString", parser);
                    parser.quary.params.forEach(param -> {
                        d.ci("addHandlerFile", "param", param);
                    });
                }
                var request = TS_SHttpHandlerRequest.of(httpExchange, parser);
                if (!fileHandlerConfig.allow.validate(request)) {
                    return;
                }
                fileHandler.handle(httpExchange);
            }
        });
    }

    private static void addHandlerRedirect(HttpServer httpServer, TS_SHttpConfigNetwork network) {
        httpServer.createContext("/", httpExchange -> {
            try (httpExchange) {
                var uri = TS_SHttpUtils.getURI(httpExchange).orElse(null);
                if (uri == null) {
                    d.ce("handle.redirect", "ERROR sniff url from httpExchange is null");
                    TS_SHttpUtils.sendError404(httpExchange);
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
    }

    public static boolean of(TS_SHttpConfigNetwork network, TS_SHttpConfigSSL ssl, TS_SHttpConfigHandlerFile fileHandler, TS_SHttpHandlerAbstract... customHandlers) {
        return TGS_UnSafe.call(() -> {
            if (fileHandler.root != null && !TS_DirectoryUtils.isExistDirectory(fileHandler.root)) {
                d.ce("of", "ERROR: fileHandler.root not exists", fileHandler.root);
                return false;
            }
            var sslContext = createSSLContext(ssl); //create ssl server
            if (sslContext == null) {
                d.ce("of", "ERROR: createSSLContext returned null");
                return false;
            }
            var httpsServer = createServer(network, sslContext);
            if (httpsServer == null) {
                d.ce("of", "ERROR: createServer returned null");
                return false;
            }
            if (fileHandler.root != null) {
                addHandlerFile(httpsServer, fileHandler);
            }
            addHanders(httpsServer, customHandlers);
            start(httpsServer);
            d.ci("of", "server started", network);
            if (!ssl.redirectToSSL) {
                return true;
            }
            var httpServer = createServer(network.cloneIt().setPort(80), null);
            addHandlerRedirect(httpServer, network);
            start(httpServer);
            return true;
        }, e -> {
            d.ce("startHttpsServlet", e);
            return false;
        });
    }
}
