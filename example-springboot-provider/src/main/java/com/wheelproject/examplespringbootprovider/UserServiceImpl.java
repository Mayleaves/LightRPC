package com.wheelproject.examplespringbootprovider;

import com.wheelproject.example.common.model.User;
import com.wheelproject.example.common.service.UserService;
import com.wheelproject.rpc.springboot.starter.annotation.RpcService;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现类
 */
@Service
@RpcService
public class UserServiceImpl implements UserService {

    public User getUser(User user) {
        System.out.println("用户名：" + user.getName());
        return user;
    }
}
