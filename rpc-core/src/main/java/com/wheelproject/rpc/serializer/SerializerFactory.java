package com.wheelproject.rpc.serializer;

import com.wheelproject.rpc.spi.SpiLoader;

import java.util.HashMap;
import java.util.Map;

/**
 * 序列化工厂：用于获取序列化器对象
 */
public class SerializerFactory {
    /**
     * 序列化映射：用于实现单例
     */

    // 1. 硬编码 HashMap 存储序列化器和实现类
//    private static final Map<String,Serializer> KEY_SERIALIZER_MAP = new HashMap<String,Serializer>(){{
//        KEY_SERIALIZER_MAP.put(SerializerKeys.JDK,new JdkSerializer());
//        KEY_SERIALIZER_MAP.put(SerializerKeys.JSON,new JsonSerializer());
//        KEY_SERIALIZER_MAP.put(SerializerKeys.HESSIAN,new HessianSerializer());
//        KEY_SERIALIZER_MAP.put(SerializerKeys.KRYO,new KryoSerializer());
//    }};

    // 2. SPI 加载指定序列化器对象
    // 静态代码块加载
    static {
        // 工厂首次加载时，调用load方法加载序列化接口所有实现类，再用getInstance获取实现类对象
        SpiLoader.load(Serializer.class);
    }

    /**
     * 默认序列化器
     */
    // 1. 硬编码
//    private static final Serializer DEFAULT_SERIALIZER = KEY_SERIALIZER_MAP.get("jdk");
    // 2. 指定序列化器对象
    private static final Serializer DEFAULT_SERIALIZER = new JdkSerializer();

    /**
     * 获取序列化器实列
     */
    // 1. 硬编码
//    public static Serializer getInstance(String key){return KEY_SERIALIZER_MAP.getOrDefault(key, DEFAULT_SERIALIZER);}
    // 2. 指定序列化器对象
    public static Serializer getInstance(String key){return SpiLoader.getInstance(Serializer.class,key);}
}
