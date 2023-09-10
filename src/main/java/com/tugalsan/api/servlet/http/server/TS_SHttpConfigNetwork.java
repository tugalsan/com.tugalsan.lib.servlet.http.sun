package com.tugalsan.api.servlet.http.server;

public class TS_SHttpConfigNetwork {

    private TS_SHttpConfigNetwork(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }
    public String ip;
    public int port;

    public static TS_SHttpConfigNetwork of(String ip, int port) {
        return new TS_SHttpConfigNetwork(ip, port);
    }

    @Override
    public String toString() {
        return TS_SHttpConfigNetwork.class.getSimpleName() + "{" + "ip=" + ip + ", port=" + port + '}';
    }
}
