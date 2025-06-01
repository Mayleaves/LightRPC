package com.wheelproject.example.provider;

import com.wheelproject.example.common.service.UserService;
import com.wheelproject.rpc.registry.LocalRegistry;
import com.wheelproject.rpc.RpcApplication;

/**
 * 简易服务提供者示例
 */
public class EasyProviderExample {
    public static void main(String[] args){
        // RPC 框架初始化
        RpcApplication.init();

        // 注册服务：服务名、服务实现类
        LocalRegistry.register(UserService.class.getName(), UserServiceImpl.class);

        // 启动 web 服务
//        HttpServer httpServer = new VertxHttpServer();  // pom.xml 中有 rpc-easy 才可以运行
//        httpServer.doStart(8080);
//        httpServer.doStart(RpcApplication.getRpcConfig().getServerPort());  // pom.xml 中有 rpc-easy 才可以运行
    }
}
