package com.wheelproject.rpc;

import com.wheelproject.rpc.config.RpcConfig;
import com.wheelproject.rpc.constant.RpcConstant;
import com.wheelproject.rpc.utils.ConfigUtils;
import lombok.extern.slf4j.Slf4j;
import com.wheelproject.rpc.config.RegistryConfig;
import com.wheelproject.rpc.registry.Registry;
import com.wheelproject.rpc.registry.RegistryFactory;

/**
 * RPC 框架应用
 * 相当于 Holder，存放了项目全局用到的变量。双检锁单例模式实现
 */
@Slf4j
public class RpcApplication {

    private static volatile RpcConfig rpcConfig; // volatile 确保多线程环境下，实例的可见性

    /**
     * 框架初始化，支持传入自定义配置
     * @param newRpcConfig 带参数的静态初始化方法
     */
    public static void init(RpcConfig newRpcConfig) {
        rpcConfig = newRpcConfig;
        log.info("rpc init, config = {}", newRpcConfig.toString());
        //注册中心初始化
        RegistryConfig registryConfig = rpcConfig.getRegistryConfig();
        Registry registry = RegistryFactory.getInstance(registryConfig.getRegistry());
        registry.init(registryConfig);
        log.info("registry init,config = {}", registryConfig);
        //（JVM 退出时执行操作）创建并注册 ShutdownHook
        Runtime.getRuntime().addShutdownHook(new Thread(registry::destroy));
    }

    /**
     * 初始化配置
     */
    public static void init() {
        RpcConfig newRpcConfig;
        try {
            newRpcConfig = ConfigUtils.loadConfig(RpcConfig.class, RpcConstant.DEFAULT_CONFIG_PREFIX);
        } catch (Exception e) {
            // 容错机制：配置加载失败，使用默认值
            newRpcConfig = new RpcConfig();
        }
        init(newRpcConfig);
    }

    /**
     * 获取配置
     * @return RPC 的配置
     */
    public static RpcConfig getRpcConfig() {
        // 第一次检查：如果实例已经创建，直接返回
        if (rpcConfig == null) {
            // 加锁
            synchronized (RpcApplication.class) {
                // 第二次检查：进入同步块后，仍需检查实例是否已创建
                if (rpcConfig == null) {
                    init();
                }
            }
        }
        return rpcConfig;
    }
}
