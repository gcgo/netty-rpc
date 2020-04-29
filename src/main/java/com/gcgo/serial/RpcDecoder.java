package com.gcgo.serial;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class RpcDecoder extends ByteToMessageDecoder {
    private Serializer serializer;
    private Class<?> clazz;

    public RpcDecoder(Class<?> clazz, Serializer serializer) {
        this.clazz = clazz;
        this.serializer = serializer;
    }

    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        byteBuf.markReaderIndex();//保存当前读指针
        int len = byteBuf.readInt();//获取数据长度
        //如果已经接受的字节长度和我们实际需要的字节长度不一致，通过调用 resetReaderIndex 来重置了 ByteBuf 的读位置
        if (byteBuf.readableBytes() < len) {
            byteBuf.resetReaderIndex();//如果长度不对，恢复读指针
            return;
        }
        //没问题就可以解码
        byte[] bytes = new byte[len];
        // 将读取的字节填充到空的数组中
        byteBuf.readBytes(bytes);
        Object o = serializer.deserialize(clazz, bytes);
        list.add(o);
        System.out.println("解码器运行了");
    }
}
