package com.wheelproject.rpc.loadbalancer;

/**
 * 负载均衡器键名常量
 *
 */
public interface LoadBalancerKeys {

    /**
     * 轮询、随机、一致性哈希
     */
    String ROUND_ROBIN = "roundRobin";

    String RANDOM = "random";

    String CONSISTENT_HASH = "consistentHash";

}
