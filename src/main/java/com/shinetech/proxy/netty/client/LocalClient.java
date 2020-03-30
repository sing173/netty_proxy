package com.shinetech.proxy.netty.client;

import com.shinetech.proxy.netty.common.Constant;
import com.shinetech.proxy.netty.common.HttpUtils;
import com.shinetech.proxy.netty.message.Message;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * 简易的netty客户端，负责与本地服务端进行交互
 * 负责管理rte客户端，通过rte客户端请求决策
 */
public class LocalClient {

    private static final Logger logger = LoggerFactory.getLogger(LocalClient.class);

    private Bootstrap bootstrap;
    private Channel channel;
    private EventLoopGroup workerGroup;

    private String host;
    private int port;

    private int clientThread = 20;
    private int heartIntervalMs = 3000;


    private LocalClient(String host, int port, int clientThread, int heartIntervalMs) {
        this.host = host;
        this.port = port;
        this.clientThread = clientThread;
        this.heartIntervalMs = heartIntervalMs;

    }


    /**
     * 根据主机名或ip地址构建客户端连接
     *
     * @param host 主机名
     * @param port 端口号
     * @return
     */
    public static LocalClient build(String host, int port, int clientThread, int heartIntervalMs) {

        return new LocalClient(host, port, clientThread, heartIntervalMs);
    }


    /**
     * 断开连接
     */
    public void disconnect() {
        try {
            if (!isClosed()) {
                channel.close().syncUninterruptibly();
            }
            workerGroup.shutdownGracefully();
            workerGroup = null;
            channel = null;
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

    }


    /**
     * 连接到服务端
     * @return
     */
    public LocalClient start() {

        workerGroup = new NioEventLoopGroup(clientThread);

        bootstrap = new Bootstrap();
        bootstrap.group(workerGroup);
        bootstrap.channel(NioSocketChannel.class);

        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel _channel) throws Exception {
                // 当客户端的所有ChannelHandler中 15s内没有read and write事件，则会触发userEventTriggered方法
//                _channel.pipeline().addLast("idleHandler", new IdleStateHandler(0, heartIntervalMs, 0, TimeUnit.MILLISECONDS));

                _channel.pipeline().addLast(new HttpClientCodec());
                _channel.pipeline().addLast(new HttpObjectAggregator(1024*1024));

                // 业务事件处理（为了不阻塞io线程，使用业务线程
                _channel.pipeline().addLast(new DefaultEventExecutorGroup(10),
                        new LocalClientHandler(LocalClient.this));
            }
        });
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.option(ChannelOption.TCP_NODELAY, true);

        //连接server
        doConnect();

        return this;
    }


    public void doConnect() {
        if (channel != null && channel.isActive()) {
            return;
        }

        ChannelFuture future = bootstrap.connect(host, port);

        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture futureListener) throws Exception {
                if (futureListener.isSuccess()) {
                    channel = futureListener.channel();
                    logger.info("connect LOCAL server success.........."+channel);
                    check();
                    //链接成功后发送一条空消息，服务端根据这个注册本地客户端
                    DefaultFullHttpRequest req = HttpUtils.request(new Message(), Constant.LOCAL_CLIENT_CONNENT);
                    channel.writeAndFlush(req);
                    logger.info("netty LOCAL client connection, host: {}, port: {}", host, port);

                } else {
                    logger.error("connect LOCAL server fail..........");

                    futureListener.channel().eventLoop().schedule(new Runnable() {
                        //一次性操作
                        @Override
                        public void run() {
                            doConnect();
                        }
                    }, 15, TimeUnit.SECONDS);
                }
            }
        });
    }

    public void check() {
        logger.info("channel is: active: " + channel.isActive() + " ,open: " + channel.isOpen() + ", writeable: " + channel.isWritable() + ",register: " + channel.isRegistered());
    }

    /**
     * 判断是否关闭，中间因为网络原因中断了连接，但是对象还在
     * @return
     */
    public boolean isClosed() {
        return channel == null || !channel.isOpen() || !channel.isWritable();
    }



}
