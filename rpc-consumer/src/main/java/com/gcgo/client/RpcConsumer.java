package com.gcgo.client;

import com.gcgo.serial.RpcRequest;

import java.lang.reflect.Proxy;
import java.util.UUID;

public class RpcConsumer {
    //1.创建代理对象
    //providerName:UserService#sayHello#
    public Object createProxy( Class<?> serviceClass,  String providerName) {

        return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class<?>[]{serviceClass}, (proxy, method, args) -> {
                    //设置参数
                    //启动类传递的是字符串："UserService#sayHello#"
                    //现在封装成RpcRequest
                    RpcRequest rpcRequest = new RpcRequest();
                    rpcRequest.setClassName(serviceClass.getName());
                    String[] split = providerName.split("#");
                    rpcRequest.setMethodName(split[1]);
                    rpcRequest.setParameters(args);
                    Class<?>[] parameterTypes = new Class[args.length];
                    for (int i = 0; i < args.length; i++) {
                        parameterTypes[i] = args[i].getClass();
                    }
                    rpcRequest.setParameterTypes(parameterTypes);
                    //随机设置了一个ID，暂时没用
                    rpcRequest.setRequestId(UUID.randomUUID().toString());
                    //设置到userClientHandler中
                    //调用初始化netty客户端方法
                    Client client = new Client("127.0.0.1",9998);
                    //去服务端请求结果
                    return client.send(rpcRequest);
                });
    }

}
