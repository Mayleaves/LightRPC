package com.wheelproject.rpc.proxy;

import com.wheelproject.rpc.constant.MessageConstant;
import com.wheelproject.rpc.exception.InvocationFailedException;
import com.wheelproject.rpc.exception.NoServiceAddressException;
import com.wheelproject.rpc.fault.retry.RetryStrategy;
import com.wheelproject.rpc.fault.retry.RetryStrategyFactory;
import com.wheelproject.rpc.fault.tolerant.TolerantStrategy;
import com.wheelproject.rpc.fault.tolerant.TolerantStrategyFactory;
import com.wheelproject.rpc.loadbalancer.LoadBalancer;
import com.wheelproject.rpc.loadbalancer.LoadBalancerFactory;
import com.wheelproject.rpc.model.RpcRequest;
import com.wheelproject.rpc.model.RpcResponse;
import com.wheelproject.rpc.serializer.Serializer;
import com.wheelproject.rpc.config.RpcConfig;
import com.wheelproject.rpc.registry.Registry;
import com.wheelproject.rpc.registry.RegistryFactory;
import com.wheelproject.rpc.model.ServiceMetaInfo;
import com.wheelproject.rpc.serializer.SerializerFactory;
import com.wheelproject.rpc.RpcApplication;
import com.wheelproject.rpc.constant.RpcConstant;

import java.util.HashMap;
import java.util.List;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.core.collection.CollUtil;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import com.wheelproject.rpc.server.tcpServer.NettyTcpClient;
import com.wheelproject.rpc.server.tcpServer.VertxTcpClient;

/**
 * 服务代理（JDK 动态代理）
 */
public class ServiceProxy implements InvocationHandler {

    /**
     * 调用代理
     *
     * @return
     * @throws Throwable
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        // 构造请求
        String serviceName = method.getDeclaringClass().getName();
        RpcRequest rpcRequest = RpcRequest.builder()
                .serviceName(serviceName)
                .methodName(method.getName())
                .parameterTypes(method.getParameterTypes())
                .args(args)
                .build();
        try {

            // 从注册中心获取服务提供者地址
            RpcConfig rpcConfig = RpcApplication.getRpcConfig();
            Registry registry = RegistryFactory.getInstance(rpcConfig.getRegistryConfig().getRegistry());
            ServiceMetaInfo serviceMetaInfo = new ServiceMetaInfo();
            serviceMetaInfo.setServiceName(serviceName);
            serviceMetaInfo.setServiceVersion(RpcConstant.DEFAULT_SERVICE_VERSION);
            List<ServiceMetaInfo> serviceMetaInfoList = registry.serviceDiscovery(serviceMetaInfo.getServiceKey());
            // 在这一句之后打断点，否则会出现 RuntimeException。
            if (CollUtil.isEmpty(serviceMetaInfoList)) {
                throw new NoServiceAddressException(MessageConstant.NO_SERVICE_ADDRESS);
            }

            // 负载均衡
            LoadBalancer loadBalancer = LoadBalancerFactory.getInstance(rpcConfig.getLoadBalancer());
            // 将调用方法名为（请求路径）作为负载均衡参数
            HashMap<String, Object> requestParams = new HashMap<>();
            requestParams.put("methodName", rpcRequest.getMethodName());
            ServiceMetaInfo selectedServiceMetaInfo = loadBalancer.select(requestParams, serviceMetaInfoList);

            RpcResponse rpcResponse;
            try{
                // 重试机制
                RetryStrategy retryStrategy = RetryStrategyFactory.getInstance(rpcConfig.getRetryStrategy());
                rpcResponse = retryStrategy.doRetry(() ->
                        // 1. 发送 Vertx/Netty HTTP 请求
//                        doHttpRequest(rpcRequest, selectedServiceMetaInfo)
                        // 2.1 发送 Vertx TCP 请求
                        VertxTcpClient.doRequest(rpcRequest, selectedServiceMetaInfo)
                        // 2.2 发送 Netty TCP 请求
//                        NettyTcpClient.doRequest(rpcRequest, selectedServiceMetaInfo)
                );
            } catch (Exception e){
                // 容错机制
                TolerantStrategy tolerantStrategy = TolerantStrategyFactory.getInstance(rpcConfig.getTolerantStrategy());
                rpcResponse = tolerantStrategy.doTolerant(null, e);
            }

            return rpcResponse.getData();
        } catch (Exception e) {
            throw new InvocationFailedException(MessageConstant.INVOCATION_FAILED);
        }
    }

    /**
     * 发送 Vertx/Netty HTTP 请求
     */
    private static RpcResponse doHttpRequest(RpcRequest rpcRequest, ServiceMetaInfo selectedServiceMetaInfo) throws IOException {
        // 自定义实现 SPI
        final Serializer serializer = SerializerFactory.
                getInstance(RpcApplication.getRpcConfig().getSerializer());
        // 序列化
        byte[] bodyBytes = serializer.serialize(rpcRequest);
        // 发送 HTTP 请求
        try (HttpResponse httpResponse = HttpRequest.post(selectedServiceMetaInfo.getServiceAddress())
                .body(bodyBytes)
                .execute()) {
            byte[] result = httpResponse.bodyBytes();
            // 反序列化
            RpcResponse rpcResponse = serializer.deserialize(result, RpcResponse.class);
            return rpcResponse;
        }
    }
}
