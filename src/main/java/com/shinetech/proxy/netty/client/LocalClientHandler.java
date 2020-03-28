package com.shinetech.proxy.netty.client;

import com.alibaba.fastjson.JSON;
import com.shinetech.proxy.netty.common.Constant;
import com.shinetech.proxy.netty.common.HttpUtils;
import com.shinetech.proxy.netty.common.buffer.RteClientResponseCache;
import com.shinetech.proxy.netty.message.DecisionMessageBody;
import com.shinetech.proxy.netty.message.Message;
import com.shinetech.proxy.netty.message.MessageHeader;
import com.shinetech.rte.netty.client.RteClient;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpConstants;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * 客户端响应请求的内部处理程序
 */

public class LocalClientHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(LocalClientHandler.class);

    private AtomicInteger roundRobin = new AtomicInteger(0);
    private static final int MAX_VALUE = 100000;
    private static final int MIN_VALUE = 0;

    private LocalClient client;
    private RteClientResponseCache responseCache;
    private LocalClientZkWatcher processor;


    public LocalClientHandler(LocalClient client) {
        this.client = client;
        this.responseCache = RteClientResponseCache.newBuild();
        this.processor = new LocalClientZkWatcher(this.responseCache);

    }

    public RteClient getClient() {
        if (processor.keys == null || processor.keys.size() == 0) {
            return null;
        }
        int index = this.getRoundRobinValue().get() % processor.keys.size();
        logger.info("from the client pool get index :" + index);
        return processor.rteClientPool.get(processor.keys.get(index));
    }

    /**
     * 软负载获取rte客户端
     * @return
     */
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
        //重新连接服务器
        client.doConnect();
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

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        logger.debug("localClient Read:" + msg);

        try {
            if (msg instanceof FullHttpResponse) {
                FullHttpResponse fullHttpResponse = (FullHttpResponse) msg;
                String jsonStr = fullHttpResponse.content().toString(HttpConstants.DEFAULT_CHARSET);
                Message message = JSON.parseObject(jsonStr, Message.class);

                //生成消息序列号，创建缓存，等待异步回调后通过序列号拿结果
                String msgNo = RandomStringUtils.randomAlphabetic(10);
                message.getHeader().setMsgNo(msgNo);
                responseCache.createStub(msgNo);
                //决策请求
                if (Constant.DECISION_REQUEST.equals(message.getHeader().getPath())) {
                    getDecisionResponseSync(ctx, message, msgNo);

                }
                //规则发布
                else if(Constant.RULE_CONFIG_REQUEST.equals(message.getHeader().getPath())) {


                }


            } else {
                throw new Exception();
            }


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 下发决策请求并同步获取结果
     * @param ctx
     * @param message
     */
    private void getDecisionResponseSync(ChannelHandlerContext ctx, Message message, String msgNo) {
        try {
            logger.debug(ctx.channel().id().asShortText() + " localClient Send:" + message);
            //随机获取rte客户端
            RteClient rteClient = getClient();
            rteClient.sendData(message, Constant.DECISION_REQUEST);

            //阻塞等待返回....
            Message result = (Message) responseCache.getResult(msgNo, 100);
            logger.debug("getDecisionResponseSync :" + result);
            //TODO 解析结果
            if(result != null) {
                result.getHeader().setPath(Constant.DECISION_RESPONSE);
                ctx.writeAndFlush(HttpUtils.request(result, Constant.DECISION_RESPONSE));
            } else {
                ctx.writeAndFlush(HttpUtils.request(message, Constant.DECISION_RESPONSE));

            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }


}
