package com.wheelproject.example.consumer;

import com.wheelproject.example.common.model.User;
import com.wheelproject.example.common.service.UserService;
import com.wheelproject.rpc.proxy.ServiceProxyFactory;

/**
 * 简易服务消费者示例
 */
public class EasyConsumerExample {
    public static void main(String[] args){
        // todo 需要获取 UserService 的实现类对象
        UserService userService = null;
        User user = new User();
        user.setName("ky");
        // 调用
        User newUser = userService.getUser(user);
        if (newUser != null)
            System.out.println(newUser.getName());
        else {
            System.out.println("user == null");
        }
    }
}
