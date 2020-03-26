package com.shinetech.proxy.netty.server;

import com.shinetech.proxy.netty.common.IProcessor;
import com.shinetech.proxy.netty.server.dispatch.DecisionRequestProcessor;
import com.shinetech.proxy.netty.server.dispatch.DecisionResponseProcessor;
import com.shinetech.proxy.netty.server.dispatch.RuleConfigRequestProcessor;
import com.shinetech.proxy.netty.server.dispatch.HttpRequestDispatcher;
import com.shinetech.proxy.netty.server.handler.HttpRequestHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.BindException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 *     http netty服务端
 *
 *     1 http netty服务端接收上游系统客户端请求（短链接）
 *
 *     2 收到决策/发布规则等请求后主动推送到客户端
 *
 *     3 客户端收到请求后主动转发到引擎的netty服务端（长链接）
 *
 *     4 引擎处理完后通过其netty服务端结果回调到客户端
 *
 *     5 客户端收到回调后再回调http netty服务端
 *
 *     6 最后由http netty服务端把结果回调到上游客户系统并删除channel
 *
 * @author luomingxing
 */
public class HttpServer {

    private static final Logger logger = LoggerFactory.getLogger(HttpServer.class);

    /**
     * 主要负责netty Accept事件并分发到worker，不用多，一般跟cpu核心数
     */
    private int bossThread = 2;
    /**
     * 负责io等事件
     */
    private int workerThread = 20;

    private int startPort = 8889;
    private boolean isStarted;
    private String hostAddress;
    private String hostAndPort;


    private Channel channel;
    private EventLoopGroup bossGroup;
    private NioEventLoopGroup workerGroup;

    /**
     * 请求分发器
     */
    private HttpRequestDispatcher requestDispatcher;


    private HttpServer(int startPort) {
        if(startPort > 0) {
            this.startPort = startPort;
        }

        this.requestDispatcher = new HttpRequestDispatcher();
        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            logger.error(e.getMessage());
        }
    }


    /**
     * 初始化类,设置监听端口
     *
     * @param startPort
     * @return
     */
    public static HttpServer build(int startPort) {
        HttpServer httpServer = new HttpServer(startPort);
        httpServer.addRequestProcessor(new DecisionRequestProcessor(),
                new RuleConfigRequestProcessor(),
                new DecisionResponseProcessor());

        return httpServer;
    }

    /**
     * 处理请求线程数（主要是io）
     *
     * @param threadNum
     * @return
     */
    public HttpServer workerThread(int threadNum) {
        this.workerThread = threadNum;
        return this;
    }

    /**
     * 连接处理线程数,一般同cpu核数（主要是accept）
     *
     * @param threadNum
     * @return
     */
    public HttpServer bossThread(int threadNum) {
        this.bossThread = threadNum;
        return this;
    }


    /**
     * 添加请求处理类
     *
     * @param processors
     * @return
     */
    public HttpServer addRequestProcessor(IProcessor... processors) {
        for (IProcessor p : processors) {
            requestDispatcher.addProcessor(p);
            logger.info("Request dispatch path: /" + p.path());
        }
        return this;
    }


    /**
     * 启动服务
     *
     * @throws Exception
     */
    public int start() throws Exception {

        bossGroup = new NioEventLoopGroup(bossThread);
        workerGroup = new NioEventLoopGroup(workerThread);
        if (isStarted) {
            logger.info("server has started, http://{}:{}", hostAddress, startPort);
            return startPort;
        }

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel _channel) throws Exception {
                            // 空闲检测
//                            _channel.pipeline().addLast(new IMIdleStateHandler());
                            // http 编/解码 或者使用HttpRequestDecoder & HttpResponseEncoder
                            _channel.pipeline().addLast(new HttpServerCodec());

//                            // http 消息聚合器,请求头和请求体等会聚合在一起
                            _channel.pipeline().addLast(new HttpObjectAggregator(1024*1024));
                            // http 请求处理
                            _channel.pipeline().addLast(new HttpRequestHandler(requestDispatcher));

                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 2048)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            boolean isBinding = true;
            ChannelFuture future = null;
            //绑定试10次, 如果不行不能启动
            int times = 0;
            int bindTime = 10;
            while (isBinding && times < bindTime) {
                try {
                    logger.info("start binding server port: " + startPort);
                    future = bootstrap.bind(startPort).sync();
                    isBinding = false;
                } catch (Exception e) {
                    logger.warn(e.getMessage() + " ,port :" + startPort);
                    times++;
                }
            }
            if (times >= bindTime && isBinding) {
                logger.error("bind address too many times: " + times + " ,start server fail. ");
                throw new BindException("bind address too many times: " + times + " ,start server fail. ");
            }

            if (future != null){
                channel = future.channel();
            }

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new Exception(e);
        }

        isStarted = true;
        logger.info("netty  server started, {}:{} , bossThread:" + bossThread + ", workerThread:" + workerThread, hostAddress, startPort);
        hostAndPort = hostAddress + ":" + startPort;
        return startPort;
    }

    public void sync() throws InterruptedException {
        this.channel.closeFuture().sync();
    }

    public int getStartPort() {
        return startPort;
    }

    public String hostAddressAndPort() {
        return hostAndPort;
    }

    /**
     * 停止服务
     */
    public void stop() {
        try {
            logger.info("netty  server stopping, {}:{}", InetAddress.getLocalHost().getHostAddress(), startPort);
            if (null == channel) {
                logger.error("server channel is null");
            } else {
                sync();
            }
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            bossGroup = null;
            workerGroup = null;
            channel = null;
            isStarted = false;
        } catch (UnknownHostException e) {
            logger.error(e.getMessage(), e);
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }

    }
}
