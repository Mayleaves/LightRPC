package com.wheelproject.example.consumer;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.wheelproject.example.common.model.User;
import com.wheelproject.example.common.service.UserService;
import com.wheelproject.rpc.model.RpcRequest;
import com.wheelproject.rpc.model.RpcResponse;
import com.wheelproject.rpc.serializer.JdkSerializer;
import com.wheelproject.rpc.serializer.Serializer;

import java.io.IOException;

/**
 * 用户服务静态代理
 * 实现 UserService 接口和 getUser 方法：构造 HTTP 请求调用服务提供者
 */
public class UserServiceProxy implements UserService {

    public User getUser(User user) {
        // 指定序列化器
        final Serializer serializer = new JdkSerializer();
        // 构造请求
        RpcRequest rpcRequest = RpcRequest.builder()
                .serviceName(UserService.class.getName())
                .methodName("getUser")
                .parameterTypes(new Class[]{User.class})
                .args(new Object[]{user})
                .build();

        try {
            // 序列化（Java 对象 -> 字节数组）
            byte[] bodyBytes = serializer.serialize(rpcRequest);
            // 发送请求
            try (HttpResponse httpResponse = HttpRequest.post("http://localhost:8080")  // 硬编码
                    .body(bodyBytes)
                    .execute()) {
                byte[] result = httpResponse.bodyBytes();
                // 反序列化（字节数组 -> Java 对象）
                RpcResponse rpcResponse = serializer.deserialize(result, RpcResponse.class);
                return (User) rpcResponse.getData();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
