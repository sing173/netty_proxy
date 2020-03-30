package com.shinetech.proxy.netty.server.dispatch;

import com.shinetech.proxy.netty.common.Constant;
import com.shinetech.proxy.netty.common.HttpUtils;
import com.shinetech.proxy.netty.common.IProcessor;
import com.shinetech.proxy.netty.server.ChannelSupervise;
import com.shinetech.proxy.netty.message.DecisionMessageBody;
import com.shinetech.proxy.netty.message.Message;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * 决策事件处理者,下发决策事件到本地客户端
 * in  DECISION_REQUEST
 * out DECISION_RESPONSE
 */
public class DecisionRequestProcessor implements IProcessor {

    private static final Logger logger = LoggerFactory.getLogger(DecisionRequestProcessor.class);

    public DecisionRequestProcessor() {

    }

    @Override
    public Message process(Object msg, ChannelHandlerContext ctx) {

        try {
            Message message = HttpRequestDispatcher.buildMessage((FullHttpRequest) msg, ctx, DecisionMessageBody.class);
            logger.debug("send decision msg:" + message);

            Channel client = ChannelSupervise.getClient();
            if (client == null){
                logger.error("DecisionRequestProcessor get client is null!");
                return null;
            }
            logger.debug("send decision event to client:" + client.id().asShortText());

            ChannelFuture channelFuture = client.writeAndFlush(HttpUtils.response(message))
                    .addListener((ChannelFutureListener) future -> {
                        if (future.isSuccess()) {
                            logger.debug("send decision event success!");
                        } else {
                            logger.error("send decision event Error!", future.cause());
                        }
                    });


            return message;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("DecisionRequestProcessor error", e);
        }
        return null;
    }

    @Override
    public String path() {
        return Constant.DECISION_REQUEST;
    }

}
