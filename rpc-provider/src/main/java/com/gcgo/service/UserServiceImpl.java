package com.gcgo.service;

import com.gcgo.handler.UnknownServerHandler;
import com.gcgo.ioc.annotation.MyService;
import com.gcgo.serial.RpcDecoder;
import com.gcgo.serial.RpcRequest;
import com.gcgo.serial.impl.JSONSerializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringEncoder;

@MyService
public class UserServiceImpl implements UserService {

    public String sayHello(String word) {
        //        System.out.println("调用成功--参数：" + word);
        return "调用成功--参数：" + word;
    }

    public static void startService(String hostName, Integer port) {
        //创建nioEventloopGroup实例2个
        NioEventLoopGroup bossGroup = new NioEventLoopGroup();
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            //创建启动辅助类
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)//指定服务器端监听套接字通道
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            //将一个一个的channelHandler添加到责任链上
                            pipeline.addLast(new StringEncoder());//编码还用string
                            pipeline.addLast(new RpcDecoder(RpcRequest.class, new JSONSerializer()));
                            //自定义Handler
                            pipeline.addLast(new UnknownServerHandler());
                        }
                    });
            //监听端口
            //sync()阻塞当前线程，一直到端口绑定操作完成
            ChannelFuture cf = bootstrap.bind(hostName, port).sync();
            System.out.println("netty服务器启动了。。。");
            //应用程序阻塞等待直到channel关闭
            cf.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            // 优雅退出 释放线程池资源
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            System.out.println("服务器优雅的释放了线程资源...");
        }

    }
}
