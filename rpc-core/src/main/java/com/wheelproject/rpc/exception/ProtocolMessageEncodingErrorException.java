package com.wheelproject.rpc.exception;

/**
 * 自定义异常类：协议消息编码错误
 */
public class ProtocolMessageEncodingErrorException extends RuntimeException {

    public ProtocolMessageEncodingErrorException(){}
    public ProtocolMessageEncodingErrorException(String message) {
        super(message);
    }

}
