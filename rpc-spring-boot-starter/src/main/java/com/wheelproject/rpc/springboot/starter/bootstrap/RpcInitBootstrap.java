package com.wheelproject.rpc.springboot.starter.bootstrap;

import com.wheelproject.rpc.RpcApplication;
import com.wheelproject.rpc.config.RpcConfig;
import com.wheelproject.rpc.server.httpServer.NettyHttpServer;
import com.wheelproject.rpc.server.httpServer.VertxHttpServer;
import com.wheelproject.rpc.server.tcpServer.NettyTcpServer;
import com.wheelproject.rpc.server.tcpServer.VertxTcpServer;
import com.wheelproject.rpc.springboot.starter.annotation.EnableRpc;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

/**
 * RPC 框架全局启动类
 *
 */
@Slf4j
public class RpcInitBootstrap implements ImportBeanDefinitionRegistrar {

    /**
     * Spring 初始化时执行，初始化 RPC 框架
     *
     * @param importingClassMetadata
     * @param registry
     */
    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        // 获取 EnableRpc 注解的属性值
        boolean needServer = (boolean) importingClassMetadata.getAnnotationAttributes(EnableRpc.class.getName())
                .get("needServer");

        // RPC 框架初始化（配置和注册中心）
        RpcApplication.init();

        // 全局配置
        final RpcConfig rpcConfig = RpcApplication.getRpcConfig();

        // 启动服务器
        if (needServer) {
            // 硬编码
            VertxTcpServer vertxTcpServer = new VertxTcpServer();
            vertxTcpServer.run(rpcConfig.getServerPort());
        } else {
            log.info("不启动 server");
        }

    }
}
