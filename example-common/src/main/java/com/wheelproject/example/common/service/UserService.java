package com.wheelproject.example.common.service;

import com.wheelproject.example.common.model.User;

/**
 * 用户服务
 */
public interface UserService {
    /**
     * 获取用户
     * @param user
     * @return
     */
    User getUser(User user);

    /**
     * 新方法 - 获取数字
     * 用于查看调用的是模拟服务，还是真实服务
     * @return
     */
    default short getNumber(){
        return (short)12345;
    }
}
