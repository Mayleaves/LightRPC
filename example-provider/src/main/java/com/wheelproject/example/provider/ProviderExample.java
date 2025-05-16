package com.wheelproject.example.provider;

import com.wheelproject.example.common.service.UserService;
import com.wheelproject.rpc.RpcApplication;
import com.wheelproject.rpc.registry.LocalRegistry;
import com.wheelproject.rpc.server.NettyHttpServer;
import com.wheelproject.rpc.config.RpcConfig;
import com.wheelproject.rpc.config.RegistryConfig;
import com.wheelproject.rpc.registry.Registry;
import com.wheelproject.rpc.registry.RegistryFactory;
import com.wheelproject.rpc.model.ServiceMetaInfo;

/**
 * 服务提供者示例
 */
public class ProviderExample {

    public static void main(String[] args) {
        // PRC 初始化框架
        RpcApplication.init();

        // 简易注册服务
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
        // 以下注释的两句可以不写，因为重写了 getServiceAddress 方法。但是前面两句必须写，否则：http://null:null。
//        serviceMetaInfo.setServiceAddress("http://" + rpcConfig.getServerHost() + ":" + rpcConfig.getServerPort());
//        serviceMetaInfo.setServiceAddress(rpcConfig.getServerHost() + ":" + rpcConfig.getServerPort());
        try {
            registry.register(serviceMetaInfo);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // 启动 web 服务
        NettyHttpServer httpServer = new NettyHttpServer();
        httpServer.run(RpcApplication.getRpcConfig().getServerPort());
    }
}
