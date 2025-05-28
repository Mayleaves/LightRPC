//package com.wheelproject.example.provider;
//
//import com.wheelproject.example.common.service.UserService;
//import com.wheelproject.rpc.RpcApplication;
//import com.wheelproject.rpc.registry.LocalRegistry;
//import com.wheelproject.rpc.server.httpServer.VertxHttpServer;
//import com.wheelproject.rpc.server.tcpServer.VertxTcpServer;
//import com.wheelproject.rpc.server.httpServer.NettyHttpServer;
//import com.wheelproject.rpc.config.RpcConfig;
//import com.wheelproject.rpc.config.RegistryConfig;
//import com.wheelproject.rpc.registry.Registry;
//import com.wheelproject.rpc.registry.RegistryFactory;
//import com.wheelproject.rpc.model.ServiceMetaInfo;
//import com.wheelproject.rpc.server.tcpServer.NettyTcpServer;
///**
// * 服务提供者示例
// */
//public class ProviderExample {
//
//    public static void main(String[] args) {
//        // PRC 初始化框架
//        RpcApplication.init();
//
//        // 注册服务
//        String serviceName = UserService.class.getName();
//        LocalRegistry.register(serviceName, UserServiceImpl.class);
//
//        // 注册服务到注册中心
//        RpcConfig rpcConfig = RpcApplication.getRpcConfig();
//        RegistryConfig registryConfig = rpcConfig.getRegistryConfig();
//        Registry registry = RegistryFactory.getInstance(registryConfig.getRegistry());
//        ServiceMetaInfo serviceMetaInfo = new ServiceMetaInfo();
//        serviceMetaInfo.setServiceName(serviceName);
//        serviceMetaInfo.setServiceHost(rpcConfig.getServerHost());
//        serviceMetaInfo.setServicePort(rpcConfig.getServerPort());
//        try {
//            registry.register(serviceMetaInfo);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//
//        // 启动 web 服务
//        // 1. Vertx
//        // 1.1 Http
////        VertxHttpServer httpServer = new VertxHttpServer();
////        httpServer.run(RpcApplication.getRpcConfig().getServerPort());
//        // 1.2 TCP
//        VertxTcpServer vertxTcpServer = new VertxTcpServer();
//        vertxTcpServer.run(RpcApplication.getRpcConfig().getServerPort());
//        // 2. Netty
//        // 2.1 Http
////        NettyHttpServer httpServer = new NettyHttpServer();
////        httpServer.run(RpcApplication.getRpcConfig().getServerPort());
//        // 2.2 TCP 未完成
////        NettyTcpServer nettyTcpServer = new NettyTcpServer();
////        nettyTcpServer.run(RpcApplication.getRpcConfig().getServerPort());
//    }
//}


package com.wheelproject.example.provider;

import com.wheelproject.example.common.service.UserService;
import com.wheelproject.rpc.RpcApplication;
import com.wheelproject.rpc.registry.LocalRegistry;
import com.wheelproject.rpc.server.httpServer.NettyHttpServer;
import com.wheelproject.rpc.server.tcpServer.VertxTcpServer;
import com.wheelproject.rpc.server.httpServer.VertxHttpServer;
import com.wheelproject.rpc.config.RpcConfig;
import com.wheelproject.rpc.config.RegistryConfig;
import com.wheelproject.rpc.registry.Registry;
import com.wheelproject.rpc.registry.RegistryFactory;
import com.wheelproject.rpc.model.ServiceMetaInfo;

/**
 * 服务提供者示例（支持多实例启动）
 */
public class ProviderExample {

    /**
     * 启动单个服务实例
     * @param port 服务端口号
     */
    public static void startServer(int port) {
        new Thread(() -> {
            try {
                // 初始化RPC框架
                RpcApplication.init();

                // 设置当前实例的端口
                RpcConfig rpcConfig = RpcApplication.getRpcConfig();
                rpcConfig.setServerPort(port);

                // 注册服务
                String serviceName = UserService.class.getName();
                LocalRegistry.register(serviceName, UserServiceImpl.class);

                // 注册服务到注册中心
                RegistryConfig registryConfig = rpcConfig.getRegistryConfig();
                Registry registry = RegistryFactory.getInstance(registryConfig.getRegistry());
                ServiceMetaInfo serviceMetaInfo = new ServiceMetaInfo();
                serviceMetaInfo.setServiceName(serviceName);
                serviceMetaInfo.setServiceHost(rpcConfig.getServerHost());
                serviceMetaInfo.setServicePort(port);  // 使用指定端口

                registry.register(serviceMetaInfo);

                // Vertx - TCP
//                VertxTcpServer vertxTcpServer = new VertxTcpServer();
//                System.out.println("Starting server on port: " + port);
//                vertxTcpServer.run(port);
                // Vertx - HTTP
//                VertxHttpServer httpServer = new VertxHttpServer();
//                System.out.println("Starting server on port: " + port);
//                httpServer.run(port);
                // Netty - HTTP
                NettyHttpServer httpServer = new NettyHttpServer();
                System.out.println("Starting server on port: " + port);
                httpServer.run(port);

            } catch (Exception e) {
                System.err.println("Failed to start server on port " + port);
                e.printStackTrace();
            }
        }).start();
    }

    public static void main(String[] args) {
        // 同时启动两个服务实例
        startServer(8080);  // 第一个实例
        startServer(8083);  // 第二个实例

        System.out.println("Two service instances started on ports 8080 and 8081");

        // 保持主线程运行（防止JVM退出）
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}