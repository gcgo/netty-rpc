import com.gcgo.serial.RpcDecoder;
import com.gcgo.serial.RpcEncoder;
import com.gcgo.serial.RpcRequest;
import com.gcgo.serial.impl.JSONSerializer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

public class MyTest {
    @Test
    public void test1() {
        String a = "haha";
        int b = 123;
        Object[] t = new Object[2];
        t[0] = a;
        t[1] = b;
        for (Object o : t) {
            System.out.println(o.getClass());
        }
    }

    /**
     * 测试编码器
     * @throws IOException
     */
    @Test
    public void test2() throws IOException {
        RpcRequest request = new RpcRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setClassName("com.gcgo.service.UserService");
        request.setMethodName("sayHello");
        String s = "str";
        request.setParameterTypes(new Class<?>[]{s.getClass()});
        request.setParameters(new Object[]{"success"});
        //序列化后长度应该是182，读取前面4个字节看看长度是不是182

        RpcEncoder rpcEncoder = new RpcEncoder(RpcRequest.class, new JSONSerializer());

        EmbeddedChannel channel = new EmbeddedChannel(rpcEncoder);
        channel.writeOutbound(request);//出站写数据
        ByteBuf  o = channel.readOutbound();

        Assert.assertEquals(182, o.readInt());//
        byte[] bytes = new byte[182];
        o.readBytes(bytes);
        JSONSerializer jsonSerializer = new JSONSerializer();
        RpcRequest deserialize = jsonSerializer.deserialize(RpcRequest.class,bytes);
        System.out.println(deserialize);
    }

    /**
     * 测试解码器
     * @throws IOException
     */
    @Test
    public void test3() throws IOException {
        RpcRequest request = new RpcRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setClassName("com.gcgo.service.UserService");
        request.setMethodName("sayHello");
        String s = "str";
        request.setParameterTypes(new Class<?>[]{s.getClass()});
        request.setParameters(new Object[]{"success"});

        JSONSerializer jsonSerializer = new JSONSerializer();
        byte[] serialize = jsonSerializer.serialize(request);
        System.out.println(serialize.length);

        RpcDecoder rpcDecoder = new RpcDecoder(RpcRequest.class, new JSONSerializer());

        ByteBuf buffer = Unpooled.buffer();
        EmbeddedChannel channel = new EmbeddedChannel(rpcDecoder);
        buffer.writeInt(serialize.length);
        buffer.writeBytes(serialize);
        channel.writeInbound(buffer);
        RpcRequest o = channel.readInbound();
        System.out.println(o);

    }
}
