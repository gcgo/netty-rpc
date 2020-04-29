package com.gcgo.handler;

import com.gcgo.serial.RpcRequest;
import com.gcgo.service.UserServiceImpl;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class UserServerHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //判断是否符合约定，符合则调用本地方法，返回数据
        //msg是约定的协议格式
        //msg:UserService#sayHello#are you ok?==========>以前版本
        //现在msg为封装的对象RpcRequest
//        if (msg.toString().startsWith("UserService")) {
//            UserServiceImpl userService = new UserServiceImpl();
//            String s = userService.sayHello(msg.toString()
//                    .substring(msg.toString().lastIndexOf("#") + 1));
//            ctx.writeAndFlush(s);
//        }

        if (msg instanceof RpcRequest){
            RpcRequest rpcRequest = (RpcRequest) msg;
            UserServiceImpl userService = new UserServiceImpl();
            //从rpcRequest中获取要调userService的什么方法？需要什么参数类型？具体参数是什么？
            String methodName = rpcRequest.getMethodName();
        }
    }
}
