package com.tugalsan.api.servlet.http.server;

import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.concurrent.Executors;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public class TS_SHttp {

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
    
    
    public static void startHttpFileServer(String ip, int port, Path root) {
        try {
            var server = SimpleFileServer.createFileServer(
                    new InetSocketAddress(ip, port),
                    root, OutputLevel.INFO
            );
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void startHttpServlet(String ip, int port, HandlerStr... handlers) {
        try {
            var server = HttpServer.create(new InetSocketAddress(ip, port), 2);
            Arrays.stream(handlers).forEach(handler -> server.createContext(handler.slash_path, handler));
            server.setExecutor(Executors.newCachedThreadPool(Thread.ofVirtual().factory()));
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    /*
        var ip = "127.0.0.1";
        var root = Path.of("D:", "xampp_data", "DAT", "PUB", "RES");
        var p12 = Path.of("D:", "xampp_data", "SSL", "tomcat.p12");
        var pass = "MyPass";
        var port = 8081;    
     */
    public static void startHttpsServlet(String ip, int port, Path p12, String pass, HandlerStr... handlers) {
        try {
            TS_NetworkSSLUtils.disableCertificateValidation();
            var server = HttpsServer.create(new InetSocketAddress(ip, port), 2);
            var sslContext = SSLContext.getInstance("TLS");
            // The keystore is generated using the following three files:
            //    - private_key.key
            //    - site.crt
            //    - site.ca-bundle
            // ...and using the following set of commands (and password as "password"):
            //    openssl pkcs12 -export -out keystore.pkcs12 -inkey private_key.key -certfile site.ca-bundle -in site.crt
            //    keytool -v -importkeystore -srckeystore keystore.pkcs12 -srcstoretype PKCS12 -destkeystore keystore.jks -deststoretype pkcs12

            // initialise the keystore
            var passwordCh = pass.toCharArray();
            var ks = KeyStore.getInstance("PKCS12");
            try ( var fis = new FileInputStream(p12.toAbsolutePath().toString())) {
                ks.load(fis, passwordCh);
            }

            // setup the key manager factory
            var kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, passwordCh);

            // setup the trust manager factory
            var tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(ks);

            // setup the HTTPS context and parameters
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
                @Override
                public void configure(HttpsParameters params) {
                    try {
                        // initialise the SSL context
                        var c = getSSLContext();
                        var engine = c.createSSLEngine();
                        params.setNeedClientAuth(false);
                        params.setCipherSuites(engine.getEnabledCipherSuites());
                        params.setProtocols(engine.getEnabledProtocols());

                        // Set the SSL parameters
                        var sslParameters = c.getSupportedSSLParameters();
                        params.setSSLParameters(sslParameters);

                    } catch (Exception ex) {
                        System.out.println("Failed to create HTTPS port");
                        System.out.println(ex.getMessage());
                    }
                }
            });

            Arrays.stream(handlers).forEach(handler -> server.createContext(handler.slash_path, handler));
            server.setExecutor(Executors.newCachedThreadPool(Thread.ofVirtual().factory()));
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
