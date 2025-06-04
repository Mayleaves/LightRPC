package com.wheelproject.rpc.protocol.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 协议消息结构
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProtocolMessage<T> {

    /**
     * 协议请求头实例
     */
    private Header header;

    /**
     * 协议请求体（请求或响应对象）
     */
    private T body;

    /**
     * 协议请求头的结构定义
     * 自定义二进制协议头
     */
    @Data
    public static class Header {

        /**
         * 魔数：保证安全性
         */
        private byte magic;

        /**
         * 版本号
         */
        private byte version;

        /**
         * 序列化方式/器
         */
        private byte serializer;

        /**
         * 消息类型（请求 / 响应）
         */
        private byte type;

        /**
         * 状态
         */
        private byte status;

        /**
         * 请求 id
         */
        private long requestId;

        /**
         * 消息体长度
         */
        private int bodyLength;
    }

}