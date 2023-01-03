package com.tugalsan.api.servlet.http.server;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;
import javax.net.ssl.*;
import com.sun.net.httpserver.*;
import com.sun.net.httpserver.SimpleFileServer.*;
import com.tugalsan.api.log.server.TS_Log;
import com.tugalsan.api.unsafe.client.TGS_UnSafe;

public class TS_SHttpServer {

    final private static TS_Log d = TS_Log.of(TS_SHttpServer.class);

    @Deprecated //HTTP not safe
    public static void startHttpFileServer(String ip, int port, Path root) {
        try {
            var server = SimpleFileServer.createFileServer(
                    new InetSocketAddress(ip, port),
                    root, OutputLevel.INFO
            );
            server.start();
        } catch (Exception e) {
            d.ce("startHttpFileServer", e);
        }
    }

    @Deprecated //HTTP not safe
    public static void startHttpServlet(String ip, int port, TS_SHttpHandlerAbstract... handlers) {
        try {
            var server = HttpServer.create(new InetSocketAddress(ip, port), 2);
            Arrays.stream(handlers).forEach(handler -> server.createContext(handler.slash_path, handler));
            server.setExecutor(Executors.newCachedThreadPool(Thread.ofVirtual().factory()));
            server.start();
        } catch (Exception e) {
            d.ce("startHttpServlet", e);
        }
    }

    //cd C:\me\codes\com.tugalsan\res\com.tugalsan.res.file
    //java --enable-preview --add-modules jdk.incubator.concurrent -jar target/com.tugalsan.res.file-1.0-SNAPSHOT-jar-with-dependencies.jar
//HOWTO
//    public static void main(String[] args) {
//        var ip = "127.0.0.1";
//        var root = Path.of("D:", "xampp_data", "DAT", "PUB", "RES");
//        var p12 = Path.of("D:", "xampp_data", "SSL", "tomcat.p12");
//        var pass = "MyPass";
//        var port = 8081;
//        startHttpFileServer(ip, port, root);
//        startHttpsServlet(ip, port,
//                p12, pass,
//                HandlerStr.of("/hello1", handle -> "hello1"),
//                HandlerStr.of("/hello2", handle -> "hello2")
//        );
//        startHttpServlet(ip, port,
//                HandlerStr.of("/hello1", handle -> "hello1"),
//                HandlerStr.of("/hello2", handle -> "hello2")
//        );
//        System.out.println("p12:" + p12);
//        System.out.println("port:" + port);
//    }
    // The keystore is generated using the following three files:
    //    - private_key.key
    //    - site.crt
    //    - site.ca-bundle
    // ...and using the following set of commands (and password as "password"):
    //    openssl pkcs12 -export -out keystore.pkcs12 -inkey private_key.key -certfile site.ca-bundle -in site.crt
    //    keytool -v -importkeystore -srckeystore keystore.pkcs12 -srcstoretype PKCS12 -destkeystore keystore.jks -deststoretype pkcs12
    // initialise the keystore
    private static SSLContext createSSLContext(Path p12, String pass) {
        return TGS_UnSafe.compile(() -> {
            //load keystore
            var ks = KeyStore.getInstance("PKCS12");
            try ( var fis = new FileInputStream(p12.toAbsolutePath().toString())) {
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
        return TGS_UnSafe.compile(() -> {
            var server = HttpsServer.create(new InetSocketAddress(ip, port), 2);
            server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
                @Override
                public void configure(HttpsParameters params) {
                    TGS_UnSafe.execute(() -> {
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

    public static boolean startHttpsServlet(String ip, int port, Path p12, String pass, TS_SHttpHandlerAbstract... handlers) {
        return TGS_UnSafe.compile(() -> {
            var sslContext = createSSLContext(p12, pass); //create ssl server
            if (sslContext == null) {
                return false;
            }
            var server = createServer(ip, port, sslContext);
            if (server == null) {
                return false;
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
