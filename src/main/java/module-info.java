module com.tugalsan.api.servlet.http {
    requires jdk.httpserver;
    requires com.tugalsan.api.executable;
    requires com.tugalsan.api.compiler;
    requires com.tugalsan.api.network;
    requires com.tugalsan.api.unsafe;
    exports com.tugalsan.api.servlet.http.server;
}
