module com.tugalsan.api.servlet.http {
    requires jdk.httpserver;
    requires com.tugalsan.api.runnable;
    requires com.tugalsan.api.callable;
    requires com.tugalsan.api.log;
    requires com.tugalsan.api.unsafe;
    exports com.tugalsan.api.servlet.http.server;
}
