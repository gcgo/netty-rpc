package com.gcgo.handler;

import com.gcgo.ioc.container.IocContainer;
import com.gcgo.serial.RpcRequest;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * 实际上应该一个service对应一个ServerHandler，因为请求规范可能不一样，相应数据也可能不一样
 */
public class UnknownServerHandler extends ChannelInboundHandlerAdapter {
    //ioc容器
    private Map<String, Object> ioc = IocContainer.getIoc();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //判断是否符合约定，符合则调用本地方法，返回数据
        //msg是约定的协议格式
        //msg:UserService#sayHello#are you ok?==========>以前版本
        //现在msg为封装的对象RpcRequest
        if (msg instanceof RpcRequest) {
            RpcRequest rpcRequest = (RpcRequest) msg;
            //从rpcRequest中获取要调哪个service实现类？调它的什么方法？需要什么参数类型？具体参数是什么？
            String className = rpcRequest.getClassName();
            String methodName = rpcRequest.getMethodName();//方法名
            Class<?>[] parameterTypes = rpcRequest.getParameterTypes();//参数类型
            Object[] parameters = rpcRequest.getParameters();//参数

            //直接从容器中根据类型获取实现类对象
            Object o = ioc.get(className);
            Class<?> aClass = o.getClass();
            Method method = aClass.getDeclaredMethod(methodName, parameterTypes);
            Object response = method.invoke(o, parameters);
            //写出结果
            ctx.writeAndFlush(response);
        }
    }
}
