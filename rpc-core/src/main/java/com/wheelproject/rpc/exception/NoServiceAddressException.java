package com.wheelproject.rpc.exception;

/**
 * 自定义异常类：暂无服务地址
 */
public class NoServiceAddressException extends RuntimeException {
    public NoServiceAddressException(){}
    public NoServiceAddressException(String message) {
        super(message);
    }
}

