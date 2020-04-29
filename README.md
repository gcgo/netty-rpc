# 大作业一：在基于Netty的自定义RPC的案例基础上进行改造

## 1 要求：

1. 序列化协议修改为JSON，使用fastjson作为JSON框架，并根据RpcRequest实体作为通信协议，服务端需根据客户端传递过来的RpcRequest对象通过反射，动态代理等技术，最终能够执行目标方法，返回字符串"success"。
2. 服务端的UserServiceImpl类上添加@Service注解，在启动项目时，添加到容器中。
3. 服务端要添加@SpringBootApplication注解，main方法中添加SpringApplication.run(ServerBootstrap.class, args);，进行启动扫描（注意项目启动类位置：扫描路径）
4. 服务端在收到参数，可以借助反射及动态代理（如需用到ApplicationContext对象，可以借助实现ApplicationContextAware接口获取），来调用UserServiceImpl方法，最终向客户端返回”success“即可。
5. 既然传递的是RpcRequest对象了，那么客户端的编码器与服务端的解码器需重新设置。

## 2 关键类说明：

1. 服务端启动类：

   ```java
   @SpringBootApplication
   public class BootStrap {
       public static void main(String[] args) {
           SpringApplication.run(BootStrap.class);
       }
   }
   ```

   其中注解扫描、IOC容器初始化、启动服务端监听，均在SpringApplication.run()方法中完成。

2. 客户端启动类：

   ```java
   public class ClientBootstrap {
       private static final String providerName = "UserService#sayHello#";
   
       public static void main(String[] args) throws InterruptedException {
           RpcConsumer rpcConsumer = new RpcConsumer();
           UserService proxy = (UserService) rpcConsumer.createProxy(UserService.class, providerName);
           while (true) {
               Thread.sleep(1500);
               System.out.println(proxy.sayHello("success"));
           }
       }
   }
   ```

   这里模拟每隔1500ms发起一次客户端请求，去请求服务端的UserService.sayHello()方法。

3. 代理类

   ```java
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
   ```

   代理类中只有一个方法，那就是返回一个代理对象。

   在这里首先将请求封装为RpcRequest对象，接着创建一个客户端对象来初始化客户端连接，利用客户端对象将封装的请求发送给服务端。

4. 客户端类：

   ```java
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
               return response;
           } finally {
               group.shutdownGracefully();
           }
   
       }
   }
   ```

   客户端类继承自ChannelInboundHandlerAdapter，通过自定义send()方法初始化连接，并发送请求；通过覆写channelRead()方法来接收服务端处理后的请求。

5. 编码器类

   ```java
   public class RpcEncoder extends MessageToByteEncoder {
       private Class<?> clazz;
       private Serializer serializer;
   
       public RpcEncoder(Class<?> clazz, Serializer serializer) {
           this.clazz = clazz;
           this.serializer = serializer;
       }
   
       protected void encode(ChannelHandlerContext channelHandlerContext, Object msg, ByteBuf byteBuf) throws Exception {
           if (clazz != null && clazz.isInstance(msg)) {
               byte[] bytes = serializer.serialize(msg);
               byteBuf.writeInt(bytes.length);
               byteBuf.writeBytes(bytes);
               System.out.println("编码器运行了");
           }
       }
   }
   ```

   继承自MessageToByteEncoder，并利用JSON方式序列化对象

6. 解码器类

   ```java
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
   ```

   继承自ByteToMessageDecoder，并利用JSON方式反序列化对象

   ## 3 运行结果

   运行BootStrap.main()启动服务端，运行ClientBootstrap.main()启动客户端，客户端模拟每隔1500ms发送一次请求，调用UserService.sayHello("success");

   结果详见：[**验证资料**](https://gitee.com/gcgo/netty-rpc/tree/master/%E9%AA%8C%E8%AF%81%E8%B5%84%E6%96%99)

   

