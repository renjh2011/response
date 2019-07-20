package com.aliware.tianchi;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;
/**
 * @author daofeng.xjf
 *
 * 客户端过滤器
 * 可选接口
 * 用户可以在客户端拦截请求和响应,捕获 rpc 调用时产生、服务端返回的已知异常。
 */
@Activate(group = Constants.CONSUMER)
public class TestClientFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        try{
            Result result = invoker.invoke(invocation);
            return result;
        }catch (Exception e){
            throw e;
        }
    }

    @Override
    public Result onResponse(Result result, Invoker<?> invoker, Invocation invocation) {
        String ip = invoker.getUrl().getIp();
        int port = invoker.getUrl().getPort();
        String key = ip+port;
        //初始化每个provider对应的线程池
        if(!result.hasException() && UserLoadBalance.WEIGHT_MAP.get(key)==null){
            String maxThreadPool = result.getAttachment(ip+port+"maxPool");
            UserLoadBalance.WEIGHT_MAP.put(key,new UserLoadBalance.WeightedRoundRobin(Integer.parseInt(maxThreadPool),Integer.parseInt(maxThreadPool),0L));
        }
        String avgRtt;
        if(ResponseWeightServer.SERVER_MAP.get(key)==null && (avgRtt=result.getAttachment(key+"avg_rtt"))!=null){
//            String avgRtt = result.getAttachment(key+"avg_rtt");
            String maxRtt = result.getAttachment(key+"max_rtt");
            ResponseWeightServer server = ResponseWeightServer.getResWightServer(key);
            server.setAvgResTime(Integer.parseInt(avgRtt));
            server.setMaxResTime(Integer.parseInt(maxRtt));
        }
        return result;
    }
}