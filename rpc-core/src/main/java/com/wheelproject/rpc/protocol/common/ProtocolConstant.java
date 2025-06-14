package com.wheelproject.rpc.protocol.common;

/**
 * 协议默认常量
 */
public interface ProtocolConstant {

    /**
     * 请求头长度
     */
    int MESSAGE_HEADER_LENGTH = 17;

    /**
     * 协议魔数
     */
    byte PROTOCOL_MAGIC = 0x1;

    /**
     * 协议版本号
     */
    byte PROTOCOL_VERSION = 0x1;
}
