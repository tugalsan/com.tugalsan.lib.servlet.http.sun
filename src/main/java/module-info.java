module com.tugalsan.api.servlet.http {
    requires jdk.httpserver;
//    requires com.tugalsan.api.crypto;
    requires com.tugalsan.api.function;
    requires com.tugalsan.api.tuple;
    requires com.tugalsan.api.union;
    requires com.tugalsan.api.charset;
    requires com.tugalsan.api.url;
    requires com.tugalsan.api.network;
    requires com.tugalsan.api.log;
    requires com.tugalsan.api.file;
    requires com.tugalsan.api.unsafe;
    requires com.tugalsan.api.stream;
    requires com.tugalsan.api.string;
    exports com.tugalsan.api.servlet.http.sun.server;
}
