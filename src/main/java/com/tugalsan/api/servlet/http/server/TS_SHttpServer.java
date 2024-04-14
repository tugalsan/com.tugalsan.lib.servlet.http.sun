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
import com.tugalsan.api.union.client.TGS_UnionExcuse;
import com.tugalsan.api.union.client.TGS_UnionExcuseVoid;
import com.tugalsan.api.url.client.TGS_Url;
import com.tugalsan.api.url.client.TGS_UrlUtils;
import com.tugalsan.api.url.client.parser.TGS_UrlParser;
import java.security.cert.CertificateException;

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
    private static TGS_UnionExcuse<SSLContext> createSSLContext(TS_SHttpConfigSSL sslConfig) {
        try {
            //load keystore
            var ks = KeyStore.getInstance("PKCS12");
            try (var fis = new FileInputStream(sslConfig.p12.toAbsolutePath().toString())) {
                ks.load(fis, sslConfig.pass.toCharArray());
            }

            //convert keystore to key manager factories
            var kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, sslConfig.pass.toCharArray());
            var tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(ks);

            // create ssl Context
            var sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            return TGS_UnionExcuse.of(sslContext);
        } catch (IOException | CertificateException | KeyManagementException | UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException ex) {
            return TGS_UnionExcuse.ofExcuse(ex);
        }
    }

    private static TGS_UnionExcuse<HttpServer> createHttpServer(TS_SHttpConfigNetwork networkConfig) {
        try {
            return TGS_UnionExcuse.of(networkConfig.ip == null
                    ? HttpServer.create(new InetSocketAddress(networkConfig.port), 0)
                    : HttpServer.create(new InetSocketAddress(networkConfig.ip, networkConfig.port), 0));
        } catch (IOException ex) {
            return TGS_UnionExcuse.ofExcuse(ex);
        }
    }

    private static TGS_UnionExcuse<HttpsServer> createHttpsServer(TS_SHttpConfigNetwork networkConfig, SSLContext sslContext) {
        try {
            var server = networkConfig.ip == null
                    ? HttpsServer.create(new InetSocketAddress(networkConfig.port), 0)//InetAddress.getLoopbackAddress() , 2
                    : HttpsServer.create(new InetSocketAddress(networkConfig.ip, networkConfig.port), 0);//, 2
            server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
                @Override
                public void configure(HttpsParameters params) {
                    var newEngine = getSSLContext().createSSLEngine();
                    params.setNeedClientAuth(false);
                    params.setCipherSuites(newEngine.getEnabledCipherSuites());
                    params.setProtocols(newEngine.getEnabledProtocols());
                    params.setSSLParameters(getSSLContext().getSupportedSSLParameters());
                }
            });
            return TGS_UnionExcuse.of(server);
        } catch (IOException ex) {
            return TGS_UnionExcuse.ofExcuse(ex);
        }
    }

    private static void addCustomHanders(HttpsServer httpServer, TS_SHttpHandlerAbstract... handlers) {
        Arrays.stream(handlers).forEach(handler -> {
            d.ci("addCustomHanders", handler.slash_path, handler.getClass().getSimpleName());
            httpServer.createContext(handler.slash_path, handler);
        });
    }

    private static void start(HttpServer httpServer) {
        httpServer.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        httpServer.start();
    }

    private static TGS_UnionExcuseVoid addHandlerFile(HttpsServer httpsServer, TS_SHttpConfigHandlerFile fileHandlerConfig) {
        if (fileHandlerConfig == null || fileHandlerConfig.isEmpty()) {
            return TGS_UnionExcuseVoid.ofExcuse(d.className, "addHandlerFile", "fileHandlerConfig == null || fileHandlerConfig.isEmpty()");
        }
        if (fileHandlerConfig.root != null && !TS_DirectoryUtils.isExistDirectory(fileHandlerConfig.root)) {
            return TGS_UnionExcuseVoid.ofExcuse(d.className, "addHandlerFile", "fileHandlerConfig.root != null && !TS_DirectoryUtils.isExistDirectory(fileHandlerConfig.root)");
        }
        var fileHandler = SimpleFileServer.createFileHandler(fileHandlerConfig.root);
        d.ci("addHandlerFile", "fileHandlerConfig.root", fileHandlerConfig.root);
        var wrap = new Object() {
            TGS_UnionExcuse<TGS_UrlParser> u_parser;
        };
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
                wrap.u_parser = TGS_UrlParser.of(TGS_Url.of(uri.toString()));
                if (wrap.u_parser.isExcuse()) {
                    return;
                }
                var parser = wrap.u_parser.value();
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
        if (wrap.u_parser != null && wrap.u_parser.isExcuse()) {
            return wrap.u_parser.toExcuseVoid();
        }
        return TGS_UnionExcuseVoid.ofVoid();
    }

    private static TGS_UnionExcuseVoid addHandlerRedirect(HttpServer httpServer, TS_SHttpConfigNetwork networkConfig) {
        var wrap = new Object() {
            TGS_UnionExcuse<TGS_UrlParser> u_parser;
        };
        httpServer.createContext("/", httpExchange -> {
            try (httpExchange) {
                var uri = TS_SHttpUtils.getURI(httpExchange).orElse(null);
                if (uri == null) {
                    TS_SHttpUtils.sendError404(httpExchange, "addHandlerRedirect", "ERROR sniff url from httpExchange is null");
                    return;
                }
                wrap.u_parser = TGS_UrlParser.of(TGS_Url.of(uri.toString()));
                if (wrap.u_parser.isExcuse()) {
                    return;
                }
                var parser = wrap.u_parser.value();
                parser.protocol.value = "https://";
                parser.host.port = networkConfig.port;
                var redirectUrl = parser.toString();
                d.ci("addHandlerRedirect", "redirectUrl", redirectUrl);
                httpExchange.getResponseHeaders().set("Location", redirectUrl);
                httpExchange.sendResponseHeaders(HttpURLConnection.HTTP_SEE_OTHER, -1);//responseLength = hasBody  ? 0 : -1
            }
        });
        if (wrap.u_parser.isExcuse()) {
            return wrap.u_parser.toExcuseVoid();
        }
        return TGS_UnionExcuseVoid.ofVoid();
    }

    public static TGS_UnionExcuseVoid of(TS_SHttpConfigNetwork networkConfig, TS_SHttpConfigSSL sslConfig, TS_SHttpConfigHandlerFile fileHandlerConfig, TS_SHttpHandlerAbstract... customHandlers) {
        var u_sslContext = createSSLContext(sslConfig); //create ssl server
        if (u_sslContext.isExcuse()) {
            return u_sslContext.toExcuseVoid();
        }
        var u_httpsServer = createHttpsServer(networkConfig, u_sslContext.value());
        if (u_httpsServer.isExcuse()) {
            return u_httpsServer.toExcuseVoid();
        }
        var u_addHandlerFile = addHandlerFile(u_httpsServer.value(), fileHandlerConfig);
        if (u_addHandlerFile.isExcuse()) {
            return u_addHandlerFile;
        }
        addCustomHanders(u_httpsServer.value(), customHandlers);
        start(u_httpsServer.value());
        d.ci("of", networkConfig, "httpsServer started");
        if (!sslConfig.redirectToSSL) {
            return TGS_UnionExcuseVoid.ofVoid();
        }
        var u_redirectServer = createHttpServer(networkConfig.cloneIt().setPort(80));
        if (u_redirectServer.isExcuse()) {
            return u_redirectServer.toExcuseVoid();
        }
        var u_redirect = addHandlerRedirect(u_redirectServer.value(), networkConfig);
        if (u_redirect.isExcuse()) {
            return u_redirect;
        }
        start(u_redirectServer.value());
        d.ci("of", networkConfig.cloneIt().setPort(80), "redirectServer started");
        return TGS_UnionExcuseVoid.ofVoid();
    }
}
