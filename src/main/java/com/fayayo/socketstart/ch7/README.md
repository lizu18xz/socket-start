### 阻塞io实现基础上 NIO进行改造
性能分析
CPU:取决于数据的频繁性，和数据转发的复杂性
内存:取决于客户端的数量、客户端发送数据的大小

服务器优化方案
  减少线程数
  增加线程执行繁忙状态
  客户端Buffer复用

NIO
阻塞IO和非阻塞IO

  阻塞IO线程消耗
  
  非阻塞IO
  客户端到达事件。
  建立连接 是一个事件
  有数据来 是一个事件
  
  NIO非阻塞IO
  Buffer 缓冲区(客户端发送和接收数据都通过Buffer转发进行)
  Channel  通道
  Selectors 处理客户端所有事件分发器
  
  与传统不同，写数据时候先写到buffer--> 丢给  channel，通过channel按块写.读则反之
  
  
  Selector  channel  buffer

### 改造
- 监听客户端的到达
- 接收回送客户端数据
- 转发客户端数据到其他客户端
- 多端消息处理

### 优化

- 现有线程模型

Thread--> selector
              --> channel--(读thread  写thread)
              --> channel
- 单线程模型
通过一个线程去监听selector
selector里面所有的channel都通过这一个线程进行轮训
并且在每个channel连接好后，在去在同一个selector上注册读和写的操作


- 监听与数据处理线程分离
AccepterThread  监听ServerSocketChannel的连接 并且完成连接
ProcessThread   socketChannel  放入到一个连接池




- 流程
启动客户端或者服务端 会启动 两个线程  readSelector   writerSelector   不断轮训
发送或者接收数据 都要 注册channel到这里
提供两个线程池处理事件




### 代码实现
````

IoContext.setup()
                .ioProvider(new IoSelectorProvider())//初始化IoSelectorProvider
                .start();

IoSelectorProvider:
初始化读和写的selector,初始化处理读写的线程池

        readSelector=Selector.open();
        writerSelector=Selector.open();

        inputHandlePool= Executors.newFixedThreadPool(4,new IoProviderThreadFactory("IoProvider-Input-Thread-"));
        outputHandlePool= Executors.newFixedThreadPool(4,new IoProviderThreadFactory("IoProvider-Output-Thread-"));

        // 开始输出输入的监听
        startRead();
        startWrite();


启动服务器:
public boolean start() {
        try {
            selector=Selector.open();//获取selector
            ServerSocketChannel socketChannel=ServerSocketChannel.open();//打开通道
            socketChannel.configureBlocking(false);//非阻塞
            //绑定本地端口
            socketChannel.socket().bind(new InetSocketAddress(port));
            //注册客户端到达的监听
            socketChannel.register(selector, SelectionKey.OP_ACCEPT);

            this.socketChannel=socketChannel;

            System.out.println("服务器信息：" + socketChannel.getLocalAddress().toString());
            //启动客户端的监听
            ClientListener listener =this.listener= new ClientListener();//通过一个 线程 初始化 我们的服务器服务
            listener.start();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }


selector.select() 是阻塞操作

监听连接事件，获取连接：

//单独启动一个线程 来 监听客户端的请求
private class ClientListener extends Thread 

SocketChannel socketChannel=serverSocketChannel.accept();//此处的accept一定可以直接返回 不会阻塞  ：返回的是客户端

 // 客户端构建异步线程  处理获取的socket
ClientHandler clientHandler = new ClientHandler(socketChannel, TCPServer.this );

获取到连接之后:
构造ClientHandler  处理客户端的连接

//开始监听数据,注册read事件
connector.setup(socketChannel);


处理客户端的连接通过IoSelectorProvider里面初始化的线程池进行处理

````






