package com.aliware.tianchi;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;

import java.util.HashMap;
import java.util.Map;

/**
 * @author daofeng.xjf
 *
 * 服务端过滤器
 * 可选接口
 * 用户可以在服务端拦截请求和响应,捕获 rpc 调用时产生、服务端返回的已知异常。
 */
@Activate(group = Constants.PROVIDER)
public class TestServerFilter implements Filter {
    private static volatile long TIME =System.currentTimeMillis();
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        long start = System.currentTimeMillis();
        try{
            RpcStatus.beginCount(invoker.getUrl(),invocation.getMethodName());
            Result result = invoker.invoke(invocation);
            RpcStatus.endCount(invoker.getUrl(),invocation.getMethodName(),System.currentTimeMillis()-start,true);
            return result;
        }catch (Exception e){
            RpcStatus.endCount(invoker.getUrl(),invocation.getMethodName(),System.currentTimeMillis()-start,false);
            throw e;
        }

    }

    @Override
    public Result onResponse(Result result, Invoker<?> invoker, Invocation invocation) {
        int port = invoker.getUrl().getPort();
        String ip = invoker.getUrl().getIp();
        String key = ip+port;
        Map<String,String> map = new HashMap<>(2);
        long now = System.currentTimeMillis();
        if((now-TIME)>1000){
            TIME=now;
            RpcStatus rpcStatus = RpcStatus.getStatus(invoker.getUrl(),invocation.getMethodName());
            map.put(key+"avg_rtt",rpcStatus.getAverageElapsed()+"");
            map.put(key+"max_rtt",rpcStatus.getMaxElapsed()+"");
//            System.out.println("AverageElapsed = [" + rpcStatus.getAverageElapsed() + "], MaxElapsed = [" + rpcStatus.getMaxElapsed() + "]");
            RpcStatus.removeStatus(invoker.getUrl(),invocation.getMethodName());
        }
        map.put(ip+port+"maxPool",(ServerStatus.getMaxThreadPool()+""));
        result.addAttachments(map);
        return result;
    }

}
