package com.wheelproject.rpc.serializer;

import java.io.IOException;

/**
 * 序列化器接口
 */
public interface Serializer {

    /**
     * 序列化
     * @param object：对象
     * @return
     * @param <T>：泛型方法声明
     * @throws IOException
     */
    <T> byte[] serialize(T object) throws IOException;

    /**
     * 反序列化
     * @param bytes：要反序列化的字节数组
     * @param type：反序列化后的类型
     * @return
     * @param <T>：泛型方法声明
     * @throws IOException
     */
    <T> T deserialize(byte[] bytes, Class<T> type) throws IOException;
}
