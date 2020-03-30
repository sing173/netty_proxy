package com.shinetech.proxy.netty.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.shinetech.proxy.netty.common.Constant;
import com.shinetech.proxy.netty.common.HttpUtils;
import com.shinetech.proxy.netty.common.buffer.RteClientResponseCache;
import com.shinetech.proxy.netty.message.DecisionMessageBody;
import com.shinetech.proxy.netty.message.Message;
import com.shinetech.proxy.netty.message.RuleConfigMessageBody;
import com.shinetech.rte.netty.client.RteClient;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpConstants;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private RteClientZkWatcher rteClientZkWatcher;


    public LocalClientHandler(LocalClient client) {
        this.client = client;
        this.responseCache = RteClientResponseCache.newBuild();

    }

    public RteClient getClient() {
        if (rteClientZkWatcher.keys == null || rteClientZkWatcher.keys.size() == 0) {
            return null;
        }
        int index = this.getRoundRobinValue().get() % rteClientZkWatcher.keys.size();
        logger.info("from the client pool get index :" + index);
        return rteClientZkWatcher.rteClientPool.get(rteClientZkWatcher.keys.get(index));
    }

    /**
     * 软负载获取rte客户端
     *
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
        logger.debug("connect LOCAL server:" + ctx.channel());
        super.channelActive(ctx);
        //本地客户端连接成功后才初始化rte客户端
        this.rteClientZkWatcher = new RteClientZkWatcher(this.responseCache);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        logger.error("disconnect LOCAL server:" + ctx.channel() + "try connect again....................");
        //断掉所有rte客户端，因为会重新连接
        rteClientZkWatcher.disConnectAllRteClient();

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
        logger.debug("localClient read:" + msg);

        try {
            if (msg instanceof FullHttpResponse) {
                FullHttpResponse fullHttpResponse = (FullHttpResponse) msg;
                String jsonStr = fullHttpResponse.content().toString(HttpConstants.DEFAULT_CHARSET);
                logger.debug("localClient read json:" + jsonStr);
                //只反序列号header和基础属性
                Message message = JSON.parseObject(jsonStr, Message.class);
                if(message == null) {
                    logger.error("localClient read msg is null!!!!");
                    ctx.writeAndFlush(HttpUtils.request(new Message(), Constant.DECISION_RESPONSE));
                    return;
                }

                //生成消息序列号，创建缓存，等待异步回调后通过序列号拿对应缓存，得到结果
                String msgNo = RandomStringUtils.randomAlphabetic(10);
                responseCache.createStub(msgNo);
                //根据请求路径解析body，决策请求
                if (Constant.DECISION_REQUEST.equals(message.getHeader().getPath())) {
                    message = JSON.parseObject(jsonStr, new TypeReference<Message<DecisionMessageBody>>() {});
                }
                //规则发布
                else if (Constant.RULE_CONFIG_REQUEST.equals(message.getHeader().getPath())) {
                    message = JSON.parseObject(jsonStr, new TypeReference<Message<RuleConfigMessageBody>>() {});
                }
                message.getHeader().setMsgNo(msgNo);

                //随机获取rte客户端
                RteClient rteClient = getClient();
                logger.debug(rteClient.channel.id().asShortText() + " RTE Client Send:" + message);
                //通过rte客户端发送数据到引擎
                rteClient.sendData(message, Constant.DECISION_REQUEST);

                //阻塞等待返回....
                Message result = (Message) responseCache.getResult(message.getHeader().getMsgNo(), 100);
                logger.debug("get RTE response sync :" + result);
                //TODO 解析结果
                if (result != null) {
                    result.getHeader().setPath(Constant.DECISION_RESPONSE);


                    ctx.writeAndFlush(HttpUtils.request(result, Constant.DECISION_RESPONSE));
                } else {
                    //TODO  超时处理
                    ctx.writeAndFlush(HttpUtils.request(message, Constant.DECISION_RESPONSE));

                }

            } else {
                throw new Exception();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }


}
