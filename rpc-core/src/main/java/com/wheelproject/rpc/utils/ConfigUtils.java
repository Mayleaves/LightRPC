package com.wheelproject.rpc.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.setting.dialect.Props;

/**
 * 配置工具类
 *
 */
public class ConfigUtils {

    /**
     * 加载配置对象
     *
     * @param tClass
     * @param prefix
     * @param <T> 泛型方法声明
     * @return
     */
    public static <T> T loadConfig(Class<T> tClass, String prefix) {
        return loadConfig(tClass, prefix, "");
    }

    /**
     * 加载配置对象，支持区分环境，如 application-prod.properties 表示生产环境、 application-test.properties 表示测试环境
     *
     */
    public static <T> T loadConfig(Class<T> tClass, String prefix, String environment) {
        // 动态构建配置文件名，基础名称是 "application"
        StringBuilder configFileBuilder = new StringBuilder("application");
        // 如果 environment 参数不为空且不为 null
        if (StrUtil.isNotBlank(environment)) {
            // 将环境名追加到配置文件名中，如 application-prod
            configFileBuilder.append("-").append(environment);
        }
        // 最终配置文件名将以 .properties 结尾
        configFileBuilder.append(".properties");
        Props props = new Props(configFileBuilder.toString());
        return props.toBean(tClass, prefix);
    }
}
