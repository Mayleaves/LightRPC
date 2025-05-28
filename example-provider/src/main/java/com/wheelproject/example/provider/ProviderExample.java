package com.wheelproject.example.provider;

import com.wheelproject.example.common.service.UserService;
import com.wheelproject.rpc.RpcApplication;
import com.wheelproject.rpc.registry.LocalRegistry;
import com.wheelproject.rpc.server.httpServer.VertxHttpServer;
import com.wheelproject.rpc.server.tcpServer.VertxTcpServer;
import com.wheelproject.rpc.server.httpServer.NettyHttpServer;
import com.wheelproject.rpc.config.RpcConfig;
import com.wheelproject.rpc.config.RegistryConfig;
import com.wheelproject.rpc.registry.Registry;
import com.wheelproject.rpc.registry.RegistryFactory;
import com.wheelproject.rpc.model.ServiceMetaInfo;
import com.wheelproject.rpc.server.tcpServer.NettyTcpServer;
/**
 * 服务提供者示例
 */
public class ProviderExample {

    public static void main(String[] args) {
        // PRC 初始化框架
        RpcApplication.init();

        // 注册服务
        String serviceName = UserService.class.getName();
        LocalRegistry.register(serviceName, UserServiceImpl.class);

        // 注册服务到注册中心
        RpcConfig rpcConfig = RpcApplication.getRpcConfig();
        RegistryConfig registryConfig = rpcConfig.getRegistryConfig();
        Registry registry = RegistryFactory.getInstance(registryConfig.getRegistry());
        ServiceMetaInfo serviceMetaInfo = new ServiceMetaInfo();
        serviceMetaInfo.setServiceName(serviceName);
        serviceMetaInfo.setServiceHost(rpcConfig.getServerHost());
        serviceMetaInfo.setServicePort(rpcConfig.getServerPort());
        try {
            registry.register(serviceMetaInfo);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 启动 web 服务
        // 1. Vertx
        // 1.1 Http
//        VertxHttpServer httpServer = new VertxHttpServer();
//        httpServer.run(RpcApplication.getRpcConfig().getServerPort());
        // 1.2 TCP
        VertxTcpServer vertxTcpServer = new VertxTcpServer();
        vertxTcpServer.run(RpcApplication.getRpcConfig().getServerPort());
        // 2. Netty
        // 2.1 Http
//        NettyHttpServer httpServer = new NettyHttpServer();
//        httpServer.run(RpcApplication.getRpcConfig().getServerPort());
        // 2.2 TCP 未完成
//        NettyTcpServer nettyTcpServer = new NettyTcpServer();
//        nettyTcpServer.run(RpcApplication.getRpcConfig().getServerPort());
    }
}

