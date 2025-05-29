package com.wheelproject.example.provider;

import com.wheelproject.example.common.service.UserService;
import com.wheelproject.rpc.RpcApplication;
import com.wheelproject.rpc.bootstrap.ProviderBootstrap;
import com.wheelproject.rpc.model.ServiceRegisterInfo;
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

import java.util.ArrayList;
import java.util.List;

/**
 * 服务提供者示例
 */
public class ProviderExample {

    public static void main(String[] args) {
        // 要注册的服务
        List<ServiceRegisterInfo<?>> serviceRegisterInfoList = new ArrayList<>();
        ServiceRegisterInfo<UserService> serviceRegisterInfo = new ServiceRegisterInfo<>(UserService.class.getName(), UserServiceImpl.class);
        serviceRegisterInfoList.add(serviceRegisterInfo);

        // 服务提供者初始化
        ProviderBootstrap.init(serviceRegisterInfoList);
    }
}

