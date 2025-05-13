package com.wheelproject.rpc.config;

import lombok.Data;
import com.wheelproject.rpc.serializer.SerializerKeys;

/**
 * RPC 框架全局配置
 *
 */
@Data
public class RpcConfig {

    /**
     * 名称
     */
    private String name = "light-rpc";

    /**
     * 版本号
     */
    private String version = "1.0";

    /**
     * 服务器主机名
     */
    private String serverHost = "localhost";

    /**
     * 服务器端口号
     */
    private Integer serverPort = 8080;

    /**
     * 模拟调用
     */
    private boolean mock = false;

    /**
     * 序列化器
     */
    private String serializer = SerializerKeys.JDK;

}
