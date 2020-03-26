package com.shinetech.proxy.netty.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.shinetech.common.utils.PropertyUtils;
import com.shinetech.common.utils.ZKClient;
import com.shinetech.proxy.netty.common.Constant;
import com.shinetech.proxy.netty.common.HttpUtils;
import com.shinetech.proxy.netty.common.buffer.RteClientResponseCache;
import com.shinetech.proxy.netty.message.Message;
import com.shinetech.rte.netty.client.RteClient;
import com.shinetech.rte.netty.client.RteClientStart;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * 客户端响应请求的内部处理程序
 */

public class LocalClientHandler extends ChannelInboundHandlerAdapter implements Watcher {


    private static final Logger logger = LoggerFactory.getLogger(LocalClientHandler.class);

    /**
     * 链接决策引擎的客户端集合
     */
    private ConcurrentMap<String, RteClient> rteClientPool = new ConcurrentHashMap<>();
    private List<String> keys = new ArrayList<>();
    private AtomicInteger roundRobin = new AtomicInteger(0);
    private static final int MAX_VALUE = 100000;
    private static final int MIN_VALUE = 0;

    private LocalClient client;
    private RteClientResponseCache responseCache;
    private String zkAddress;
    private ZKClient zkClient;

    public LocalClientHandler(LocalClient client) {
        this.client = client;
        Properties properties = PropertyUtils.getInstance();
        this.zkAddress = (String) properties.get(Constant.ZOOKEEPER_ADDRESS);
        this.responseCache = RteClientResponseCache.newBuild();

        try {
            this.zkClient = new ZKClient(zkAddress, this);

            //不存在则新建, 这个目录必须存在
            if (!zkClient.exist(Constant.ZK_PROXY_CONFIG_PATH)) {
                zkClient.registerService(Constant.ZK_PROXY_CONFIG_PATH, "0");
                logger.info("create zk address：" + Constant.ZK_PROXY_CONFIG_PATH);
            }

            initClientPool();
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("Zookeeper init error：" + e.getMessage());
        }

        //启动一个线程，处理zk连接断开的情况
        Runnable sendTask = new Runnable() {
            @Override
            public void run() {
                for (; ; ) {
                    logger.debug("check zookeeper Client  connection ......");
                    // 如果zk连接不上，重新初始化zk
                    if (!zkClient.isAvailable()) {
                        if (zkClient.reconnect(LocalClientHandler.this)) {
                            initClientPool();
                        }
                    }
                    //休眠
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        new Thread(sendTask).start();
    }

    public void initClientPool() {
        // 带有事件监听，创建或者删除下一层节点，都会被通知到
        List<String> oldList = zkClient.listServices(Constant.SPOUT_NETTY_SERVER, true);

        if (oldList != null && !oldList.isEmpty()) {
            logger.info("flink source netty server register info: " + oldList);

            ConcurrentMap<String, RteClient> tmpClientPool = new ConcurrentHashMap<>();
            List<String> tmpKeys = new ArrayList<>();

            for (String key : oldList) {
                String ip = "";
                String port = "";
                try {
                    ip = key.split(":")[0];
                    port = key.split(":")[1];
                } catch (Exception e) {
                    continue;//不是ip:port的格式，进入下一个循环
                }

                RteClient client = null;
                // 避免重复创建连接，只要ip和端口一样，会有15s重连机制
                if (rteClientPool.containsKey(key)) {
                    client = rteClientPool.get(key);
                } else {
                    client = RteClientStart.buildClient(ip, Integer.parseInt(port), 20, 15000, responseCache);
                }

                logger.info("add flink znode:" + ip + ":" + port);

                tmpClientPool.put(key, client);
                tmpKeys.add(key);
            }

            // 直接变量替换，避免直接操作的冲突
            rteClientPool = tmpClientPool;
            keys = tmpKeys;


        } else {
            logger.error("storm netty spout server is null in zk: " + zkAddress + " , path: " + Constant.SPOUT_NETTY_SERVER);
        }
    }

    @Override
    public void process(WatchedEvent event) {
        String path = event.getPath();
        logger.info("zookeeper path has changed" + path);

        if (Constant.ZK_PROXY_CONFIG_PATH.equalsIgnoreCase(path)) {
            initClientPool();
        }
    }

    public RteClient getClient() {
        if (keys == null || keys.size() == 0) {
            return null;
        }
        int index = this.getRoundRobinValue().get() % keys.size();
        logger.info("from the client pool get index :" + index);
        return rteClientPool.get(keys.get(index));
    }


    private AtomicInteger getRoundRobinValue() {
        if (this.roundRobin.getAndIncrement() > MAX_VALUE) {
            this.roundRobin.set(MIN_VALUE);
        }
        return this.roundRobin;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.debug("connect server:" + ctx.channel());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        logger.error("disconnect server:" + ctx.channel() + "try connect again....................");
        client.doConnect();  //重新连接服务器
    }

    /**
     * 一段时间未进行读写操作 发送心跳
     *
     * @param ctx
     * @param evt
     * @throws Exception
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.WRITER_IDLE) {
                logger.info("long time no io，heartbeat.............." + Thread.currentThread().toString());
                ctx.channel().writeAndFlush(heartBeat());
            }
        }
    }

    private Message heartBeat() {
        return new Message();
    }


    /**
     * 从 ByteBuf 里面根据头部的消息类型解析出 Message
     *
     * @param ctx
     * @param buf
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        logger.debug("localClient Read:" + msg);

        try {
            if (msg instanceof FullHttpResponse) {
                FullHttpResponse fullHttpResponse = (FullHttpResponse) msg;
                String jsonStr = fullHttpResponse.content().toString(HttpConstants.DEFAULT_CHARSET);
                Message message = JSON.parseObject(jsonStr, Message.class);

                if (Constant.DECISION_REQUEST.equals(message.getHeader().getPath())) {
                    sendResponse(ctx, message);


                } else if(Constant.DECISION_RESPONSE.equals(message.getHeader().getPath())){



                }


            } else {
                throw new Exception();
            }


        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private void sendResponse(ChannelHandlerContext ctx, Message message) {
        try {
            // 生成随机的序列
            String serialKey = RandomStringUtils.randomAlphabetic(10);
            message.getHeader().setMsgNo(serialKey);
            responseCache.createStub(serialKey);

            logger.debug("localClient Send:" + message);

            RteClient rteClient = getClient();
            rteClient.sendData(message, Constant.DECISION_REQUEST);

            //阻塞等待返回....
            String result = responseCache.getResult(serialKey, 50);
            Message messageResponse = JSON.parseObject(result, Message.class);

            ctx.writeAndFlush(HttpUtils.request(messageResponse, Constant.DECISION_RESPONSE));

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }


}
