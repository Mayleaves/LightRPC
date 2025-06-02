package com.wheelproject.rpc.spi;

import cn.hutool.core.io.resource.ResourceUtil;
import com.wheelproject.rpc.serializer.Serializer;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SPI 加载器
 */
@Slf4j
public class SpiLoader {
    /**
     * 系统 SPI 目录
     */
    private static final String RPC_SYSTEM_SPI_DIR = "META-INF/rpc/system/";
    /**
     * 用户自定义SPI目录
     */
    private static final String RPC_CUSTOM_SPI_DIR = "META-INF/rpc/custom/";
    /**
     * 扫描路径
     */
    private static final String[] SCAN_DIRS = new String[]{RPC_SYSTEM_SPI_DIR, RPC_CUSTOM_SPI_DIR};
    /**
     * 动态加载的类列表
     */
    private static final List<Class<?>> LOAD_CLASS_LIST = Collections.singletonList(Serializer.class);
    /**
     * 存储已经加载的类：<接口名, <键名, 实现类>>
     */
    private static final Map<String, Map<String, Class<?>>> loaderMap = new ConcurrentHashMap<String, Map<String, Class<?>>>();
    /**
     * 对象实列缓存：单例模式，<类路径, 对象实例>
     */
    private static final Map<String, Object> instanceCache = new ConcurrentHashMap<String, Object>();

    /**
     * 加载所有类型
     * 不推荐，更推荐 load 加载指定类型
     */
    public static void loadAll() {
        log.info("加载所有 SPI");
        for (Class<?> aClass : LOAD_CLASS_LIST) {
            load(aClass);
        }
    }

    /**
     * 获取某个接口的实例
     *
     */
    public static <T> T getInstance(Class<?> tClass, String key) {
        String tClassName = tClass.getName();
        Map<String, Class<?>> keyClassMap = loaderMap.get(tClassName);
        if (keyClassMap == null) {
            throw new RuntimeException(String.format("SpiLoader未加载 %s 类型...", tClassName));
        }
        if (!keyClassMap.containsKey(key)) {
            throw new RuntimeException(String.format("SpiLoader的 %s，不存在key = %s 类型...", tClassName, key));
        }
        // 获取到要加载的实现类型
        Class<?> implClass = keyClassMap.get(key);
        // 尝试从实例缓存中加载指定类型实列
        String implClassName = implClass.getName();
        if (!instanceCache.containsKey(implClassName)) {  // 实现类不在缓存
            try {
                instanceCache.put(implClassName, implClass.newInstance());  // 放入缓存
            } catch (InstantiationException | IllegalAccessException e) {
                String errorMsg = "Sorry, %s 类 实列化失败...";
                throw new RuntimeException(errorMsg, e);
            }
        }
        return (T) instanceCache.get(implClassName);
    }

    /**
     * 加载某个类型
     *
     */
    public static Map<String, Class<?>> load(Class<?> loadClass) {
        log.info("加载类型为：{} 的 SPI", loadClass.getName());
        // 扫描路径：用户自定义 SPI 优先级高于系统 SPI
        Map<String, Class<?>> keyClassMap = new HashMap<>();
        for (String scanDir : SCAN_DIRS) {
            // 通过 ResourceUtil.getResources 获取配置文件，不是通过文件路径获取。因为文件路径可能正确获取。
            List<URL> resources = ResourceUtil.getResources(scanDir + loadClass.getName());
            // 依次读取资源文件
            for (URL resource : resources) {
                try {
                    InputStreamReader inputStreamReader = new InputStreamReader(resource.openStream());
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        String[] strArray = line.split("=");
                        if (strArray.length > 1) {
                            String key = strArray[0];
                            String className = strArray[1];
                            keyClassMap.put(key, Class.forName(className));
                        }
                    }
                } catch (Exception e) {
                    log.error("spi resource load error", e);
                }
            }
        }
        loaderMap.put(loadClass.getName(), keyClassMap);
        return keyClassMap;
    }
}
