package com.shinetech.rte.netty.client;



import com.alibaba.fastjson.JSONObject;
import com.shinetech.proxy.netty.common.HttpUtils;
import com.shinetech.proxy.netty.message.Message;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 *  调用决策引擎的netty客户端
 */
public class RteClient {

    private static final Logger logger = LoggerFactory.getLogger(RteClient.class);


    private Bootstrap bootstrap;
    private Channel channel;
    private EventLoopGroup workerGroup;


    //默认是10个线程
    private int clientThread = 20;
    private int heartIntervalMs = 15000;//默认15s心跳
    private String host;
    private int port;



    private RteClient(String host, int port, int clientThread, int heartIntervalMs) {
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
    public static RteClient build(String host, int port, int clientThread, int heartIntervalMs) {
        return new RteClient(host, port, clientThread, heartIntervalMs);
    }



    public void check() {
        logger.info("channel is: active: " + channel.isActive() + " ,open: " + channel.isOpen() + ", writeable: " + channel.isWritable() + ",register: " + channel.isRegistered());
    }


    public RteClient start() throws Exception {

        workerGroup = new NioEventLoopGroup(clientThread);

        bootstrap = new Bootstrap();
        bootstrap.group(workerGroup);
        bootstrap.channel(NioSocketChannel.class);

        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel _channel) throws Exception {
                // 当客户端的所有ChannelHandler中 15s内没有read and write事件，则会触发userEventTriggered方法
//                _channel.pipeline().addLast("idleHandler", new IdleStateHandler(0, 0, heartIntervalMs, TimeUnit.MILLISECONDS));

                _channel.pipeline().addLast(new HttpClientCodec());
                _channel.pipeline().addLast(new HttpObjectAggregator(1024*1024));

                // 内部处理逻辑
                _channel.pipeline().addLast(new RteClientHandler(host, port, RteClient.this));// in bount
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
            public void operationComplete(ChannelFuture futureListener) throws Exception {
                if (futureListener.isSuccess()) {
                    channel = futureListener.channel();
                    logger.info("连接rte服务端成功..........");
                    logger.info("netty client connection establish, host: {}, port: {}", host, port);
                } else {
                    logger.error("连接rte服务端失败..........");

                    futureListener.channel().eventLoop().schedule(new Runnable() {//一次性操作
                        @Override
                        public void run() {
                            doConnect();
                        }
                    }, 15, TimeUnit.SECONDS);
                }
            }
        });
    }


    // 判断是否关闭，中间因为网络原因中断了连接，但是对象还在
    public boolean isClosed() {
        return channel == null || !channel.isOpen() || !channel.isWritable();
    }






    /**
     * 客户端发送数据
     * @param message
     * @param uri
     * @return
     */
    public ChannelFuture sendData(final Message message, String uri) {
        try {
            DefaultFullHttpRequest req = HttpUtils.request(message, this.host + ":" + this.port + uri);
            return channel.writeAndFlush(req);
        } catch (UnsupportedEncodingException e) {
            logger.error(e.getMessage(), e);
        } catch (URISyntaxException e) {
            logger.error(e.getMessage(), e);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }










}
