package com.wheelproject.rpc.server.httpServer;

/**
 * HTTP 服务器接口
 */
public interface HttpServer {
    /**
     * 启动服务器
     * @param port
     */
    void run(int port);
}
