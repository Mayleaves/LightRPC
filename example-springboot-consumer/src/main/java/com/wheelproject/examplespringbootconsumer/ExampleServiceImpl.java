package com.wheelproject.examplespringbootconsumer;

import com.wheelproject.example.common.model.User;
import com.wheelproject.example.common.service.UserService;
import com.wheelproject.rpc.bootstrap.ConsumerBootstrap;
import com.wheelproject.rpc.fault.retry.RetryStrategyKeys;
import com.wheelproject.rpc.loadbalancer.LoadBalancerKeys;
import com.wheelproject.rpc.proxy.ServiceProxyFactory;
import com.wheelproject.rpc.springboot.starter.annotation.RpcReference;
import org.springframework.stereotype.Service;

/**
 * 服务消费者实例实现类
 *
 */
@Service
public class ExampleServiceImpl {

    /**
     * 使用 RPC 框架注入
     */
    @RpcReference
    private UserService userService;

    /**
     * 测试方法
     */
    public void test() {
        User user = new User();
        user.setName("ky");
        User resultUser = userService.getUser(user);
        System.out.println(resultUser.getName());
    }
}
