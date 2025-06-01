package com.wheelproject.rpc.registry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;  // 线程安全

/**
 * 本地注册中心
 */
public class LocalRegistry {

    /**
     * 注册信息存储
     *
     * String：服务名称
     * Class<?>：服务的实现类
     */
    private static final Map<String, Class<?>> map = new ConcurrentHashMap<>();

    /**
     * 注册服务
     *
     */
    public static void register(String serviceName, Class<?> implClass) {
        map.put(serviceName, implClass);
    }

    /**
     * 获取服务的实现类
     *
     */
    public static Class<?> get(String serviceName) {
        return map.get(serviceName);
    }

    /**
     * 删除服务
     *
     */
    public static void remove(String serviceName) {
        map.remove(serviceName);
    }

}
