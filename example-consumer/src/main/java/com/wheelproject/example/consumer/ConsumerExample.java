package com.wheelproject.example.consumer;

import com.wheelproject.rpc.config.RpcConfig;
import com.wheelproject.rpc.proxy.ServiceProxyFactory;
import com.wheelproject.rpc.utils.ConfigUtils;
import com.wheelproject.example.common.model.User;
import com.wheelproject.example.common.service.UserService;

import java.io.IOException;
/**
 * 服务消费者示例
 *
 */
public class ConsumerExample {

    public static void main(String[] args) {
        RpcConfig rpc = ConfigUtils.loadConfig(RpcConfig.class, "rpc");
        System.out.println(rpc);

        // 获取代理
        UserService userService = ServiceProxyFactory.getProxy(UserService.class);
        User user = new User();
        user.setName("ky");

        //调用
        User newUser = userService.getUser(user);
        if (newUser!= null){
            System.out.println(newUser.getName());
        }else {
            System.out.println("user == null");
        }
//        // 第一次调用（会查询注册中心并写入缓存）
//        System.out.println("=== 第一次调用 ===");
//        callServiceAndPrint(userService, user);
//
//        // 第二次调用（应该命中缓存）
//        System.out.println("=== 第二次调用 ===");
//        callServiceAndPrint(userService, user);
//
//        // 第三次调用前，等待用户手动停止服务提供者
//        System.out.println("=== 请手动停止服务提供者，然后按回车继续 ===");
//        waitForUserInput();
//
//        // 第三次调用（会触发监听，清空缓存后重新查询注册中心）
//        System.out.println("=== 第三次调用 ===");
//        callServiceAndPrint(userService, user);

        // 通过 Mock，调用模拟服务
        // 注意还是要启动提供者的 ProviderExample
        long number = userService.getNumber();
        System.out.println(number);  // 12345
    }
    private static void callServiceAndPrint(UserService userService, User user) {
        User result = userService.getUser(user);
        if (result != null) {
            System.out.println("调用结果: " + result.getName());
        } else {
            System.out.println("调用结果: user == null");
        }
    }

    private static void waitForUserInput() {
        try {
            System.in.read(); // 等待用户按回车
        } catch (IOException e) {
            System.err.println("等待输入时发生错误，继续执行...");
        }
    }
}
