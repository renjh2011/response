package com.aliware.tianchi;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.cluster.LoadBalance;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;


/**
 * @author daofeng.xjf
 *
 * 负载均衡扩展接口
 * 必选接口，核心接口
 * 此类可以修改实现，不可以移动类或者修改包名
 * 选手需要基于此类实现自己的负载均衡算法
 */
public class UserLoadBalance implements LoadBalance {
    private static volatile long TIME =System.currentTimeMillis();

    protected static class WeightedRoundRobin {
        private int weight;
        private AtomicLong current = new AtomicLong(0);
        /**
         * 如果长时间
         */
        private int initWeight;
        public int getWeight() {
            return weight;
        }

        public WeightedRoundRobin(int weight, Long current) {
            this.weight = weight;
            this.current.set(current);
        }
        public WeightedRoundRobin(int initWeight,int weight, Long current) {
            this.initWeight = initWeight;
            this.weight = weight;
            this.current.set(current);
        }

        public void setWeight(int weight) {
            this.weight = weight;
            current.set(0);
        }
        public long increaseCurrent() {
            return current.addAndGet(weight);
        }
        public void sel(int total) {
            current.addAndGet(-1 * total);
        }

        public void setCurrent(long current) {
            this.current.set(current);
        }

        public int getInitWeight() {
            return initWeight;
        }

        public void setInitWeight(int initWeight) {
            this.initWeight = initWeight;
        }

        @Override
        public String toString() {
            return "{" +
                    "weight=" + weight +
                    ", current=" + current +
                    '}';
        }
    }

    static ConcurrentMap<String,WeightedRoundRobin> WEIGHT_MAP = new ConcurrentHashMap<>();

    @Override
    public <T> Invoker<T> select(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        int wSize = WEIGHT_MAP.size();
        int iSize = invokers.size();
        //如果权重信息没有加载完 随机
        if(wSize!=iSize){
            Invoker<T> invoker = invokers.get(ThreadLocalRandom.current().nextInt(invokers.size()));
            return invoker;
        }

        long now = System.currentTimeMillis();
        if((now-TIME)>1000) {
            TIME = now;
            ConcurrentMap<String,ResponseWeightServer> serverMap = new ConcurrentHashMap<>(ResponseWeightServer.SERVER_MAP);
//            System.out.println(serverMap);
            ResponseWeightServer.SERVER_MAP=new ConcurrentHashMap<>();
            if(serverMap.size()==iSize) {
                int totalWeight = serverMap.values().stream().mapToInt(ResponseWeightServer::getAvgResTime).sum();
                serverMap.forEach((k, v) -> {
                    WEIGHT_MAP.put(k, new UserLoadBalance.WeightedRoundRobin(totalWeight - v.getAvgResTime(), 0L));
                });
//                System.out.println(WEIGHT_MAP);
            }
        }
        UserLoadBalance.WeightedRoundRobin selectedWRR = null;

        long maxCurrent = Long.MIN_VALUE;
        int totalWeight = 0;
        Invoker<T> selectedInvoker = null;
        //调整权重结束
        for (Invoker<T> invoker : invokers) {
            URL url1 = invoker.getUrl();
            String ip = url1.getIp();
            int port = url1.getPort();
            String key = ip + port;
            UserLoadBalance.WeightedRoundRobin weightedRoundRobin = WEIGHT_MAP.get(key);
            int weight = WEIGHT_MAP.get(key).getWeight();
            long cur = weightedRoundRobin.increaseCurrent();
            if (cur > maxCurrent) {
                maxCurrent = cur;
                selectedInvoker = invoker;
                selectedWRR = weightedRoundRobin;
            }
            totalWeight += weight;
        }
        if (selectedInvoker != null) {
            selectedWRR.sel(totalWeight);
            return selectedInvoker;
        }
        return invokers.get(ThreadLocalRandom.current().nextInt(iSize));
    }

}