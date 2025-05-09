package com.wheelproject.example.provider;

import com.wheelproject.example.common.service.UserService;
import com.wheelproject.rpc.RpcApplication;
import com.wheelproject.rpc.registry.LocalRegistry;
import com.wheelproject.rpc.server.NettyHttpServer;

/**
 * 简易版启动 提供者
 */
public class ProviderExample {

    public static void main(String[] args) {
        //初始化框架
        RpcApplication.init();

        //简易注册服务
        LocalRegistry.register(UserService.class.getName(), UserServiceImpl.class);

        NettyHttpServer httpServer = new NettyHttpServer();
        httpServer.run(RpcApplication.getRpcConfig().getServerPort());
    }
}
