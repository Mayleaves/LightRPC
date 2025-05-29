package com.wheelproject.rpc.fault.tolerant;

import com.wheelproject.rpc.model.RpcResponse;

import java.util.Map;

/**
 * 容错策略通用接口
 *
 */
public interface TolerantStrategy {

    /**
     * 容错
     *
     * @param context 上下文，用于传递数据
     * @param e       异常
     * @return
     */
    RpcResponse doTolerant(Map<String, Object> context, Exception e);
}
