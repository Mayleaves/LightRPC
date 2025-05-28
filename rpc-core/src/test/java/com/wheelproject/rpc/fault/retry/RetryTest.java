package com.wheelproject.rpc.fault.retry;

import com.wheelproject.rpc.fault.retry.NoRetryStrategy;
import com.wheelproject.rpc.fault.retry.RetryStrategy;
import com.wheelproject.rpc.model.RpcResponse;
import org.junit.Test;

/**
 * 重试策略测试
 */
public class RetryTest {

    RetryStrategy retryStrategy = new NoRetryStrategy();
//    RetryStrategy retryStrategy = new FixedIntervalRetryStrategy();

    @Test
    public void doRetry(){
        try {
            System.out.println("开始测试...");
            RpcResponse rpcResponse = retryStrategy.doRetry(() -> {
                System.out.println("模拟调用...");
                // 1. 模拟重试成功
                RpcResponse response = new RpcResponse();
                return response;  // 返回成功响应
                // 2. 模拟重试失败
//                throw new RuntimeException("模拟重试失败");
            });
            System.out.println(rpcResponse);
        }catch (Exception e){
            System.out.println("重试失败...");
            e.printStackTrace();
        }
    }
}

