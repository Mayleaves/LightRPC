package com.wheelproject.rpc.fault.retry;

import com.github.rholder.retry.*;
import com.wheelproject.rpc.model.RpcResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * 固定时间间隔 - 重试策略
 *
 */
@Slf4j
public class FixedIntervalRetryStrategy implements RetryStrategy{

    @Override
    public RpcResponse doRetry(Callable<RpcResponse> callable) throws ExecutionException,RetryException {
        Retryer<RpcResponse> retryer = RetryerBuilder.<RpcResponse>newBuilder()
                .retryIfExceptionOfType(Exception.class)  // 重试条件
                .withWaitStrategy(WaitStrategies.fixedWait(3L, TimeUnit.SECONDS))  // 重试等待
                .withStopStrategy(StopStrategies.stopAfterAttempt(3))  // 重试停止
                .withRetryListener(new RetryListener() {  // 重试工作
                    @Override
                    public <V> void onRetry(Attempt<V> attempt) {
                        log.info("重试次数：{}", attempt.getAttemptNumber());
                    }
                })
                .build();
        return retryer.call(callable);
    }
}
