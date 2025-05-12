package com.wheelproject.example.consumer;

import com.wheelproject.rpc.config.RpcConfig;
import com.wheelproject.rpc.proxy.ServiceProxyFactory;
import com.wheelproject.rpc.utils.ConfigUtils;
import com.wheelproject.example.common.model.User;
import com.wheelproject.example.common.service.UserService;
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

        // 通过 Mock，调用模拟服务
        // 注意还是要启动提供者的 ProviderExample
        long number = userService.getNumber();
        System.out.println(number);
    }
}
