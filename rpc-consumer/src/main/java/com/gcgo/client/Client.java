package com.gcgo.client;

import com.gcgo.serial.RpcEncoder;
import com.gcgo.serial.RpcRequest;
import com.gcgo.serial.impl.JSONSerializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;

public class Client extends ChannelInboundHandlerAdapter {
    private final Object obj = new Object();
    /*RPC响应对象*/
    private Object response;
    /*主机名*/
    private String host;
    /*端口*/
    private int port;

    public Client(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        this.response = msg;
//        System.out.println("获得了结果");
        synchronized (obj) {
            //收到响应，唤醒线程
            obj.notifyAll();
//            System.out.println("唤醒线程");
        }
    }

    public Object send(RpcRequest request) throws Exception {
        //创建nioEventloopGroup实例1个
        NioEventLoopGroup group = new NioEventLoopGroup();
        try {
            //创建bootstrap
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel channel) {
                            ChannelPipeline pipeline = channel.pipeline();
                            pipeline.addLast(new RpcEncoder(RpcRequest.class, new JSONSerializer()));
                            pipeline.addLast(new StringDecoder());//解码还用string
                            pipeline.addLast(Client.this);
                        }
                    });
            //建立连接
            ChannelFuture cf = bootstrap.connect(host, port).sync();
            System.out.println("客户端建立了连接");
            cf.channel().writeAndFlush(request).sync();

            synchronized (obj) {
                //未收到响应，使线程继续等待
//                System.out.println("等待响应");
                obj.wait();
//                System.out.println("线程被唤醒");
            }
//            if (null != response) {
//                //关闭RPC请求连接
//                cf.channel().closeFuture().sync();
//            }
            return response;
        } finally {
            //切记！！！！！！！！！！！！
            //优雅关闭是建立在前面cf.channel().writeAndFlush(request).sync();阻塞的前提下
            //要是不阻塞就直接优雅关闭，Handler中方法会一直wait，请求也不会发到服务端！！！！！！！！
            //切记！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！
            System.out.println("优雅关闭");
            group.shutdownGracefully();
        }

    }
}
