package com.wheelproject.rpc.fault.tolerant;

/**
 * 容错策略键名常量
 *
 */
public interface TolerantStrategyKeys {

    String FAIL_BACK = "failBack";

    String FAIL_FAST = "failFast";

    String FAIL_OVER = "failOver";

    String FAIL_SAFE = "failSafe";

}