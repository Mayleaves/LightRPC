package com.wheelproject.rpc.exception;

/**
 * 自定义异常类：协议消息编码错误
 * 推荐：ProtocolMessageEncodingErrorException 继承 BaseException 继承 RuntimeException；但是我这里没写 BaseException。
 * 涉及 RuntimeException 的都可以改为自定义异常类。
 */
public class ProtocolMessageEncodingErrorException extends RuntimeException {

    public ProtocolMessageEncodingErrorException(){}
    public ProtocolMessageEncodingErrorException(String message) {
        super(message);
    }

}
