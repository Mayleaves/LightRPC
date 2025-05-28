package com.wheelproject.rpc.fault.retry;

import com.wheelproject.rpc.spi.SpiLoader;

/**
 * 重试策略工厂（用于获取重试器对象）
 *
 */
public class RetryStrategyFactory {

    static {
        SpiLoader.load(RetryStrategy.class);
    }

    /**
     * 默认重试策略
     */
    private static final RetryStrategy DEFAULT_RETRY_STRATEGY = new NoRetryStrategy();

    /**
     * 获取重试机制实列
     * @param key
     * @return
     */
    public static RetryStrategy getInstance(String key){
        return SpiLoader.getInstance(RetryStrategy.class,key);
    }
}
