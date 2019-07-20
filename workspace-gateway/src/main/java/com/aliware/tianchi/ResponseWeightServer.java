package com.aliware.tianchi;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ResponseWeightServer {
    private volatile String key;
    /**
     * 服务端平均响应时间
     */
    private volatile Integer avgResTime=0;
    /**
     * 最大响应时间
     */
    private volatile Integer maxResTime=0;

    static ConcurrentMap<String,ResponseWeightServer> SERVER_MAP = new ConcurrentHashMap<>();

    private ResponseWeightServer(){

    }

    public static ResponseWeightServer getResWightServer(String key){
        ResponseWeightServer server = SERVER_MAP.get(key);
        if(server==null){
            SERVER_MAP.put(key,new ResponseWeightServer());
            server = SERVER_MAP.get(key);
        }
        return server;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Integer getAvgResTime() {
        return avgResTime;
    }

    public void setAvgResTime(Integer avgResTime) {
        this.avgResTime = avgResTime;
    }

    public Integer getMaxResTime() {
        return maxResTime;
    }

    public void setMaxResTime(Integer maxResTime) {
        this.maxResTime = maxResTime;
    }

    @Override
    public String toString() {
        return "ResponseWeightServer{" +
                "avgResTime=" + avgResTime +
                ", maxResTime=" + maxResTime +
                '}';
    }
}
