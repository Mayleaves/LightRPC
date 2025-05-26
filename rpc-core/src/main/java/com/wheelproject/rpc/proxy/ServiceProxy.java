package com.wheelproject.rpc.proxy;

import com.wheelproject.rpc.model.RpcRequest;
import com.wheelproject.rpc.model.RpcResponse;
import com.wheelproject.rpc.serializer.Serializer;
import com.wheelproject.rpc.serializer.JdkSerializer;
import com.wheelproject.rpc.serializer.HessianSerializer;
import com.wheelproject.rpc.config.RpcConfig;
import com.wheelproject.rpc.registry.Registry;
import com.wheelproject.rpc.registry.RegistryFactory;
import com.wheelproject.rpc.model.ServiceMetaInfo;
import com.wheelproject.rpc.serializer.SerializerFactory;
import com.wheelproject.rpc.RpcApplication;
import com.wheelproject.rpc.constant.RpcConstant;
import com.wheelproject.rpc.protocol.common.ProtocolMessage;
import com.wheelproject.rpc.protocol.common.ProtocolConstant;
import com.wheelproject.rpc.protocol.messageEnum.ProtocolMessageSerializerEnum;
import com.wheelproject.rpc.protocol.messageEnum.ProtocolMessageTypeEnum;
import com.wheelproject.rpc.protocol.codec.ProtocolMessageEncoder;
import com.wheelproject.rpc.protocol.codec.ProtocolMessageDecoder;

import java.util.List;
import java.util.ServiceLoader;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.core.collection.CollUtil;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import io.vertx.core.Vertx;
import io.vertx.core.net.NetClient;

import java.util.concurrent.CompletableFuture;

import cn.hutool.core.util.IdUtil;
import io.vertx.core.buffer.Buffer;

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
        // 指定序列化器
        // 1. 硬编码
//        Serializer serializer = new JdkSerializer();
        // 2. 系统实现 SPI
        /*
        Serializer serializer = null;
        ServiceLoader<Serializer> serviceLoader = ServiceLoader.load(Serializer.class);
        for (Serializer service : serviceLoader) {
            serializer = service;
        }
         */
        // 3. 自定义实现 SPI
        final Serializer serializer = SerializerFactory.getInstance(RpcApplication.getRpcConfig().getSerializer());

        // 构造请求
        String serviceName = method.getDeclaringClass().getName();
        RpcRequest rpcRequest = RpcRequest.builder()
                .serviceName(serviceName)
                .methodName(method.getName())
                .parameterTypes(method.getParameterTypes())
                .args(args)
                .build();
        try {
            // 序列化
            byte[] bodyBytes = serializer.serialize(rpcRequest);

            // 从注册中心获取服务提供者地址
            RpcConfig rpcConfig = RpcApplication.getRpcConfig();
            Registry registry = RegistryFactory.getInstance(rpcConfig.getRegistryConfig().getRegistry());
            ServiceMetaInfo serviceMetaInfo = new ServiceMetaInfo();
            serviceMetaInfo.setServiceName(serviceName);
            serviceMetaInfo.setServiceVersion(RpcConstant.DEFAULT_SERVICE_VERSION);
            List<ServiceMetaInfo> serviceMetaInfoList = registry.serviceDiscovery(serviceMetaInfo.getServiceKey());
            // 在这一句之后打断点，否则会出现 RuntimeException。
            if (CollUtil.isEmpty(serviceMetaInfoList)) {
                throw new RuntimeException("暂无服务地址");
            }
            // 暂时先取第一个
            ServiceMetaInfo selectedServiceMetaInfo = serviceMetaInfoList.get(0);

            // 1. 发送 HTTP 请求
//            try (HttpResponse httpResponse = HttpRequest.post(selectedServiceMetaInfo.getServiceAddress())
//                    .body(bodyBytes)
//                    .execute()) {
//                byte[] result = httpResponse.bodyBytes();
//                // 反序列化
//                RpcResponse rpcResponse = serializer.deserialize(result, RpcResponse.class);
//                return rpcResponse.getData();
//            }
            // 2. 发送 TCP 请求
            Vertx vertx = Vertx.vertx();
            NetClient netClient = vertx.createNetClient();
            CompletableFuture<RpcResponse> responseFuture = new CompletableFuture<>();  // 异步转同步
            netClient.connect(selectedServiceMetaInfo.getServicePort(), selectedServiceMetaInfo.getServiceHost(),
                    result -> {
                        if (result.succeeded()) {
                            System.out.println("Connected to TCP server");
                            io.vertx.core.net.NetSocket socket = result.result();
                            // 发送数据
                            // 构造消息
                            ProtocolMessage<RpcRequest> protocolMessage = new ProtocolMessage<>();
                            ProtocolMessage.Header header = new ProtocolMessage.Header();
                            header.setMagic(ProtocolConstant.PROTOCOL_MAGIC);
                            header.setVersion(ProtocolConstant.PROTOCOL_VERSION);
                            header.setSerializer((byte)
                                    ProtocolMessageSerializerEnum.getEnumByValue(RpcApplication.getRpcConfig().getSerializer()).getKey());
                            header.setType((byte) ProtocolMessageTypeEnum.REQUEST.getKey());
                            header.setRequestId(IdUtil.getSnowflakeNextId());
                            protocolMessage.setHeader(header);
                            protocolMessage.setBody(rpcRequest);
                            // 编码请求
                            try {
                                Buffer encodeBuffer = ProtocolMessageEncoder.encode(protocolMessage);
                                socket.write(encodeBuffer);
                            } catch (IOException e) {
                                throw new RuntimeException("协议消息编码错误");
                            }
                            // 接收响应
                            socket.handler(buffer -> {
                                try {
                                    ProtocolMessage<RpcResponse> rpcResponseProtocolMessage =
                                            (ProtocolMessage<RpcResponse>) ProtocolMessageDecoder.decode(buffer);
                                    // 完成了响应
                                    responseFuture.complete(rpcResponseProtocolMessage.getBody());
                                } catch (IOException e) {
                                    throw new RuntimeException("协议消息编码错误");
                                }
                            });
                        } else {
                            System.err.println("Failed to connect to TCP server");
                        }
                    });
            // 阻塞，直到响应完成，才会继续向下执行
            RpcResponse rpcResponse = responseFuture.get();
            // 记得关闭连接
            netClient.close();
            return rpcResponse.getData();
        } catch (IOException e) {
            System.err.println("反序列化失败！");
            e.printStackTrace();
        }

        return null;
    }
}
