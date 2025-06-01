package com.wheelproject.example.consumer;

import com.wheelproject.rpc.bootstrap.ConsumerBootstrap;
import com.wheelproject.rpc.proxy.ServiceProxyFactory;
import com.wheelproject.example.common.model.User;
import com.wheelproject.example.common.service.UserService;

import java.io.IOException;
/**
 * 服务消费者示例
 *
 */
public class ConsumerExample {

    public static void main(String[] args) {
        // 服务提供者初始化
        ConsumerBootstrap.init();

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
    }
}
