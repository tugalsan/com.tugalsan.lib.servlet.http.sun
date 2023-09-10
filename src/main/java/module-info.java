module com.tugalsan.api.servlet.http {
    requires jdk.httpserver;
    requires gwt.user;
    requires com.tugalsan.api.runnable;
    requires com.tugalsan.api.callable;
    requires com.tugalsan.api.tuple;
    requires com.tugalsan.api.url;
    requires com.tugalsan.api.log;
    requires com.tugalsan.api.validator;
    requires com.tugalsan.api.file;
    requires com.tugalsan.api.unsafe;
    requires com.tugalsan.api.stream;
    exports com.tugalsan.api.servlet.http.server;
}
