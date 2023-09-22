package com.tugalsan.api.servlet.http.server;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;
import javax.net.ssl.*;
import com.sun.net.httpserver.*;
import com.tugalsan.api.charset.server.TS_CharSetUtils;
import com.tugalsan.api.file.server.TS_DirectoryUtils;
import com.tugalsan.api.file.server.TS_FileUtils;
import com.tugalsan.api.log.server.TS_Log;
import com.tugalsan.api.unsafe.client.TGS_UnSafe;
import com.tugalsan.api.url.client.TGS_Url;
import com.tugalsan.api.url.client.TGS_UrlUtils;
import com.tugalsan.api.url.client.parser.TGS_UrlParser;

public class TS_SHttpServer {

    final private static TS_Log d = TS_Log.of(false, TS_SHttpServer.class);

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

    private static HttpServer createHttpServer(TS_SHttpConfigNetwork network) {
        return TGS_UnSafe.call(() -> {
            return network.ip == null
                    ? HttpServer.create(new InetSocketAddress(network.port), 0)
                    : HttpServer.create(new InetSocketAddress(network.ip, network.port), 0);
        }, e -> {
            d.ce("createHttpServer", e);
            return null;
        });
    }

    private static HttpsServer createHttpsServer(TS_SHttpConfigNetwork network, SSLContext sslContext) {
        return TGS_UnSafe.call(() -> {
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
                    }, e -> d.ct("createHttpsServer", e));
                }
            });
            return server;
        }, e -> {
            d.ce("createHttpsServer", e);
            return null;
        });
    }

    private static void addCustomHanders(HttpsServer httpServer, TS_SHttpHandlerAbstract... handlers) {
        Arrays.stream(handlers).forEach(handler -> {
            d.ce("addCustomHanders", handler.slash_path, handler.getClass().getSimpleName());
            httpServer.createContext(handler.slash_path, handler);
        });
    }

    private static void start(HttpServer httpServer) {
        httpServer.setExecutor(Executors.newCachedThreadPool(Thread.ofVirtual().factory()));
        httpServer.start();
    }

    private static boolean addHandlerFile(HttpsServer httpsServer, TS_SHttpConfigHandlerFile fileHandlerConfig) {
        if (fileHandlerConfig == null) {
            return true;
        }
        if (fileHandlerConfig.root != null && !TS_DirectoryUtils.isExistDirectory(fileHandlerConfig.root)) {
            d.ce("of", "ERROR: fileHandler.root not exists", fileHandlerConfig.root);
            return false;
        }
        var fileHandler = SimpleFileServer.createFileHandler(fileHandlerConfig.root);
        d.ci("addHandlerFile", "fileHandlerConfig.root", fileHandlerConfig.root);
        httpsServer.createContext(fileHandlerConfig.slash_path_slash, httpExchange -> {
            try (httpExchange) {
                d.ci("addHandlerFile", "hello");
                var uri = TS_SHttpUtils.getURI(httpExchange).orElse(null);
                if (uri == null) {
                    TS_SHttpUtils.sendError404(httpExchange, "addHandlerFile", "ERROR sniff url from httpExchange is null");
                    return;
                }
                var requestPath = uri.getPath();
                if (fileHandlerConfig.filterUrlsWithHiddenChars && !TS_CharSetUtils.isPrintable_slow(requestPath)) {
                    TS_SHttpUtils.sendError404(httpExchange, "addHandlerFile", "non printable chars detected" + requestPath);
                    return;
                }
                if (!TS_FileUtils.isExistFile(Path.of(requestPath))) {
                    TS_SHttpUtils.sendError404(httpExchange, "addHandlerFile", "FileNotExists" + requestPath);
                    return;
                }
                var parser = TGS_UrlParser.of(TGS_Url.of(uri.toString()));
                if (d.infoEnable) {
                    d.ci("addHandlerFile", "parser.toString", parser);
                    parser.quary.params.forEach(param -> {
                        d.ci("addHandlerFile", "param", param);
                    });
                }
                if (TGS_UrlUtils.isHackedUrl(TGS_Url.of(parser.path.fileOrServletName))) {
                    TS_SHttpUtils.sendError404(httpExchange, "addHandlerFile", "isHackedUrl? " + parser.path.toString_url());
                    return;
                }
                var requestBall = TS_SHttpHandlerRequest.of(httpExchange, parser);
                if (!fileHandlerConfig.allow.validate(requestBall)) {
                    return;
                }
                fileHandler.handle(httpExchange);
            }
        });
        return true;
    }

    private static void addHandlerRedirect(HttpServer httpServer, TS_SHttpConfigNetwork network) {
        httpServer.createContext("/", httpExchange -> {
            try (httpExchange) {
                var uri = TS_SHttpUtils.getURI(httpExchange).orElse(null);
                if (uri == null) {
                    TS_SHttpUtils.sendError404(httpExchange, "addHandlerRedirect", "ERROR sniff url from httpExchange is null");
                    return;
                }
                var parser = TGS_UrlParser.of(TGS_Url.of(uri.toString()));
                parser.protocol.value = "https://";
                parser.host.port = network.port;
                var redirectUrl = parser.toString();
                d.ci("addHandlerRedirect", "redirectUrl", redirectUrl);
                httpExchange.getResponseHeaders().set("Location", redirectUrl);
                TGS_UnSafe.run(() -> {
                    httpExchange.sendResponseHeaders(HttpURLConnection.HTTP_SEE_OTHER, -1);//responseLength = hasBody  ? 0 : -1
                }, e -> d.ct("addHandlerRedirect", e));
            }
        });
    }

    public static boolean of(TS_SHttpConfigNetwork network, TS_SHttpConfigSSL ssl, TS_SHttpConfigHandlerFile fileHandlerConfig, TS_SHttpHandlerAbstract... customHandlers) {
        return TGS_UnSafe.call(() -> {
            var sslContext = createSSLContext(ssl); //create ssl server
            if (sslContext == null) {
                d.ce("of", "ERROR: createSSLContext returned null");
                return false;
            }
            var httpsServer = createHttpsServer(network, sslContext);
            if (httpsServer == null) {
                d.ce("of", "ERROR: createServer returned null");
                return false;
            }
            if (!addHandlerFile(httpsServer, fileHandlerConfig)) {
                return false;
            }
            addCustomHanders(httpsServer, customHandlers);
            start(httpsServer);
            d.ci("of", network, "httpsServer started");
            if (!ssl.redirectToSSL) {
                return true;
            }
            var redirectServer = createHttpServer(network.cloneIt().setPort(80));
            addHandlerRedirect(redirectServer, network);
            start(redirectServer);
            d.ci("of", network.cloneIt().setPort(80), "redirectServer started");
            return true;
        }, e -> {
            d.ce("of", e);
            return false;
        });
    }
}
