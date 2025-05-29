package com.wheelproject.rpc.bootstrap;

import com.wheelproject.rpc.RpcApplication;
import com.wheelproject.rpc.config.RegistryConfig;
import com.wheelproject.rpc.config.RpcConfig;
import com.wheelproject.rpc.model.ServiceMetaInfo;
import com.wheelproject.rpc.model.ServiceRegisterInfo;
import com.wheelproject.rpc.registry.LocalRegistry;
import com.wheelproject.rpc.registry.Registry;
import com.wheelproject.rpc.registry.RegistryFactory;
import com.wheelproject.rpc.server.httpServer.NettyHttpServer;
import com.wheelproject.rpc.server.httpServer.VertxHttpServer;
import com.wheelproject.rpc.server.tcpServer.VertxTcpServer;

import java.util.List;

/**
 * 服务提供者启动类（初始化）
 *
 */
public class ProviderBootstrap {

    /**
     * 初始化
     */
    public static void init(List<ServiceRegisterInfo<?>> serviceRegisterInfoList) {
        // RPC 框架初始化（配置和注册中心）
        RpcApplication.init();
        // 全局配置
        final RpcConfig rpcConfig = RpcApplication.getRpcConfig();

        // 注册服务
        for (ServiceRegisterInfo<?> serviceRegisterInfo : serviceRegisterInfoList) {
            String serviceName = serviceRegisterInfo.getServiceName();
            // 本地注册
            LocalRegistry.register(serviceName, serviceRegisterInfo.getImplClass());

            // 注册服务到注册中心
            RegistryConfig registryConfig = rpcConfig.getRegistryConfig();
            Registry registry = RegistryFactory.getInstance(registryConfig.getRegistry());
            ServiceMetaInfo serviceMetaInfo = new ServiceMetaInfo();
            serviceMetaInfo.setServiceName(serviceName);
            serviceMetaInfo.setServiceHost(rpcConfig.getServerHost());
            serviceMetaInfo.setServicePort(rpcConfig.getServerPort());
            try {
                registry.register(serviceMetaInfo);
            } catch (Exception e) {
                throw new RuntimeException(serviceName + " 服务注册失败", e);
            }
        }

        // 启动 web 服务
        // 1. Vertx
        // 1.1 Http
//        VertxHttpServer httpServer = new VertxHttpServer();
//        httpServer.run(RpcApplication.getRpcConfig().getServerPort());
        // 1.2 TCP
//        VertxTcpServer vertxTcpServer = new VertxTcpServer();
//        vertxTcpServer.run(RpcApplication.getRpcConfig().getServerPort());
        // 2. Netty
        // 2.1 Http
        NettyHttpServer httpServer = new NettyHttpServer();
        httpServer.run(RpcApplication.getRpcConfig().getServerPort());
        // 2.2 TCP 未完成
//        NettyTcpServer nettyTcpServer = new NettyTcpServer();
//        nettyTcpServer.run(RpcApplication.getRpcConfig().getServerPort());
    }
}
