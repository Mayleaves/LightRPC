package com.wheelproject.rpc.registry;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.cron.CronUtil;  // 单例模式
import cn.hutool.cron.task.Task;
import cn.hutool.json.JSONUtil;
import cn.hutool.core.collection.ConcurrentHashSet;
import com.wheelproject.rpc.config.RegistryConfig;
import com.wheelproject.rpc.model.ServiceMetaInfo;
import io.etcd.jetcd.*;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.PutOption;
import io.etcd.jetcd.watch.WatchEvent;

import java.nio.charset.StandardCharsets;  // 标准字符集
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Etcd 注册中心
 */
public class EtcdRegistry implements Registry {
    private Client client;

    // 对 etcd 中的键值对进行设置、获取、删除等操作。
    private KV kvClient;

    /**
     * 根节点
     */
    private static final String ETCD_ROOT_PATH = "/rpc/";

    /**
     * 过期字典：本机注册的节点 key 集合（用于维护续期）
     */
    private final Set<String> localRegisterNodeKeySet = new HashSet<>();

    /**
     * 注册中心服务缓存（只支持单个服务缓存，已废弃，请使用下方的 RegistryServiceMultiCache）
     */
    @Deprecated
    private final RegistryServiceCache registryServiceCache = new RegistryServiceCache();

    /**
     * 注册中心服务缓存（支持多个服务键）
     */
    private final RegistryServiceMultiCache registryServiceMultiCache = new RegistryServiceMultiCache();

    /**
     * 正在监听的 key 集合
     * 使用 ConcurrentHashSet 防止并发冲突
     */
    private final Set<String> watchingKeySet = new ConcurrentHashSet<>();

    @Override
    public void init(RegistryConfig registryConfig) {
        client = Client.builder()
                .endpoints(registryConfig.getAddress())
                .connectTimeout(Duration.ofMillis(registryConfig.getTimeout()))
                .build();
        kvClient = client.getKVClient();
        heartBeat();
    }

    @Override
    public void register(ServiceMetaInfo serviceMetaInfo) throws Exception {
        // 创建 Lease 和 KV 客户端
        Lease leaseClient = client.getLeaseClient();

        // 创建一个 30 秒的租约
        long leaseId = leaseClient.grant(30).get().getID();

        // 设置要存储的键值对
        String registerKey = ETCD_ROOT_PATH + serviceMetaInfo.getServiceNodeKey();
        ByteSequence key = ByteSequence.from(registerKey, StandardCharsets.UTF_8);
        ByteSequence value = ByteSequence.from(JSONUtil.toJsonStr(serviceMetaInfo), StandardCharsets.UTF_8);

        // 将键值对与租约关联起来，并设置过期时间
        PutOption putOption = PutOption.builder().withLeaseId(leaseId).build();
        kvClient.put(key, value, putOption).get();

        // 添加节点信息到本地缓存
        localRegisterNodeKeySet.add(registerKey);
    }

    @Override
    public void unRegister(ServiceMetaInfo serviceMetaInfo) {
        String registerKey = ETCD_ROOT_PATH + serviceMetaInfo.getServiceNodeKey();
        kvClient.delete(ByteSequence.from(registerKey, StandardCharsets.UTF_8));
        // 将节点信息从本地缓存移除
        localRegisterNodeKeySet.remove(registerKey);
    }

    /**
     * 服务发现逻辑
     * @param serviceKey 服务键名
     * @return 服务元信息
     */
    @Override
    public List<ServiceMetaInfo> serviceDiscovery(String serviceKey) {

        // 优先从缓存获取服务
        // 1. 单个服务缓存
//         List<ServiceMetaInfo> cachedServiceMetaInfoList = registryServiceCache.readCache();
        // 2. 支持多个服务同时缓存
        List<ServiceMetaInfo> cachedServiceMetaInfoList = registryServiceMultiCache.readCache(serviceKey);
        if (cachedServiceMetaInfoList != null) {
            return cachedServiceMetaInfoList;
        }

        // 前缀搜索，结尾一定要加 '/'
        String searchPrefix = ETCD_ROOT_PATH + serviceKey + "/";

        try {
            // 前缀查询
            GetOption getOption = GetOption.builder().isPrefix(true).build();
            List<KeyValue> keyValues = kvClient.get(
                            ByteSequence.from(searchPrefix, StandardCharsets.UTF_8),
                            getOption)
                    .get()
                    .getKvs();
            // 解析服务信息
            List<ServiceMetaInfo> serviceMetaInfoList = keyValues.stream()
                    .map(keyValue -> {
                        String key = keyValue.getKey().toString(StandardCharsets.UTF_8);
                        // 监听 key 的变化
                        watch(key);
                        String value = keyValue.getValue().toString(StandardCharsets.UTF_8);
                        return JSONUtil.toBean(value, ServiceMetaInfo.class);
                    })
                    .collect(Collectors.toList());
            // 写入服务缓存
            // 1. 单个服务缓存
//            registryServiceCache.writeCache(serviceMetaInfoList);
            // 2. 支持多个服务同时缓存
            registryServiceMultiCache.writeCache(serviceKey, serviceMetaInfoList);
            return serviceMetaInfoList;
        } catch (Exception e) {
            throw new RuntimeException("获取服务列表失败", e);
        }
    }

    @Override
    public void destroy() {
        System.out.println("当前节点下线");
        // 遍历本节点所有的 key
        for (String key : localRegisterNodeKeySet) {
            try {
                kvClient.delete(ByteSequence.from(key, StandardCharsets.UTF_8)).get();
            } catch (Exception e) {

                throw new RuntimeException(key + "节点下线失败");
            }
        }
        // 释放资源
        if (kvClient != null) {
            kvClient.close();
        }
        if (client != null) {
            client.close();
        }
    }

    @Override
    public void heartBeat() {
        // 定时任务：10 秒续签一次
        CronUtil.schedule("*/10 * * * * *", (Task) () -> {
            // 遍历本地缓存的所有 key
            for (String key : localRegisterNodeKeySet) {
                // 检查键是否过期
                try {
                    List<KeyValue> keyValues = kvClient.get(ByteSequence.from(key, StandardCharsets.UTF_8))
                            .get()
                            .getKvs();
                    // 若该节点已过期，需要重启节点才能重新注册
                    if (CollUtil.isEmpty(keyValues)) {
                        continue;
                    }
                    // 若节点未过期，重新注册（续期）
                    KeyValue keyValue = keyValues.get(0);
                    String value = keyValue.getValue().toString(StandardCharsets.UTF_8);
                    ServiceMetaInfo serviceMetaInfo = JSONUtil.toBean(value, ServiceMetaInfo.class);
                    register(serviceMetaInfo);
                } catch (Exception e) {
                    throw new RuntimeException(key + "续期失败", e);
                }
            }
        });
        // 支持秒级别定时任务
        CronUtil.setMatchSecond(true);
        CronUtil.start();
    }

    /**
     * 监听（消费端）
     *
     * @param serviceNodeKey 服务节点 key
     */
    @Override
    public void watch(String serviceNodeKey) {
        Watch watchClient = client.getWatchClient();
        // 即使 key 在注册中心被删除后再重新设置，之前的监听依旧生效。
        // 所以只监听之前未被加入到监听集合的 key，防止重复
        boolean newWatch = watchingKeySet.add(serviceNodeKey);
        if (newWatch) {
            watchClient.watch(ByteSequence.from(serviceNodeKey, StandardCharsets.UTF_8), response -> {
                for (WatchEvent event : response.getEvents()) {
                    switch (event.getEventType()) {
                        // key 删除时触发
                        case DELETE:
                            // 清理注册服务缓存
                            // 1. 单个服务缓存
//                            registryServiceCache.clearCache();
                            // 2. 支持多个服务同时缓存
                            registryServiceMultiCache.clearCache(serviceNodeKey);
                            break;
                        case PUT:
                        default:
                            break;
                    }
                }
            });
        }
    }
}