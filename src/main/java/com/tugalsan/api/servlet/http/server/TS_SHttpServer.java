package com.tugalsan.api.servlet.http.server;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;
import javax.net.ssl.*;
import com.sun.net.httpserver.*;
import com.tugalsan.api.log.server.TS_Log;
import com.tugalsan.api.unsafe.client.TGS_UnSafe;

public class TS_SHttpServer {

    final private static TS_Log d = TS_Log.of(TS_SHttpServer.class);

    // The keystore is generated using the following three files:
    //    - private_key.key
    //    - site.crt
    //    - site.ca-bundle
    // ...and using the following set of commands (and password as "password"):
    //    openssl pkcs12 -export -out keystore.pkcs12 -inkey private_key.key -certfile site.ca-bundle -in site.crt
    //    keytool -v -importkeystore -srckeystore keystore.pkcs12 -srcstoretype PKCS12 -destkeystore keystore.jks -deststoretype pkcs12
    // initialise the keystore
    private static SSLContext createSSLContext(Path p12, String pass) {
        return TGS_UnSafe.call(() -> {
            //load keystore
            var ks = KeyStore.getInstance("PKCS12");
            try (var fis = new FileInputStream(p12.toAbsolutePath().toString())) {
                ks.load(fis, pass.toCharArray());
            }

            //convert keystore to key manager factories
            var kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, pass.toCharArray());
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

    private static HttpsServer createServer(String ip, int port, SSLContext sslContext) {
        return TGS_UnSafe.call(() -> {
            var server = ip == null
                    ? HttpsServer.create(new InetSocketAddress(port), 0)//InetAddress.getLoopbackAddress() , 2
                    : HttpsServer.create(new InetSocketAddress(ip, port), 0);//, 2
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

    private static void addHanders(HttpsServer server, TS_SHttpHandlerAbstract... handlers) {
        Arrays.stream(handlers).forEach(handler -> server.createContext(handler.slash_path, handler));
    }

    private static void start(HttpsServer server) {
        server.setExecutor(Executors.newCachedThreadPool(Thread.ofVirtual().factory()));
        server.start();
    }

    public static boolean startHttpsServlet(int port, Path p12, String pass, Path fileHandlerRoot, TS_SHttpHandlerAbstract... handlers) {
        return startHttpsServlet(null, port, p12, pass, fileHandlerRoot, handlers);
    }

    public static boolean startHttpsServlet(String ip, int port, Path p12, String pass, Path fileHandlerRoot, TS_SHttpHandlerAbstract... handlers) {
        return TGS_UnSafe.call(() -> {
            var sslContext = createSSLContext(p12, pass); //create ssl server
            if (sslContext == null) {
                return false;
            }
            var server = createServer(ip, port, sslContext);
            if (server == null) {
                return false;
            }
            if (fileHandlerRoot != null) {
                server.createContext("/file", SimpleFileServer.createFileHandler(fileHandlerRoot));
            }
            addHanders(server, handlers);
            start(server);
            return true;
        }, e -> {
            d.ce("startHttpsServlet", e);
            return false;
        });
    }
}
