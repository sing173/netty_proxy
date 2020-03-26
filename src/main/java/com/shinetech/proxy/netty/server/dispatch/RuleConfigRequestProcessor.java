package com.shinetech.proxy.netty.server.dispatch;

import com.shinetech.proxy.netty.common.Constant;
import com.shinetech.proxy.netty.common.HttpUtils;
import com.shinetech.proxy.netty.common.IProcessor;
import com.shinetech.proxy.netty.server.ChannelSupervise;
import com.shinetech.proxy.netty.message.Message;
import com.shinetech.proxy.netty.message.RuleConfigMessageBody;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 规则发布事件处理
 * in  RULE_CONFIG_REQUEST
 * out RULE_CONFIG_RESPONSE
 */
public class RuleConfigRequestProcessor implements IProcessor {

    private static final Logger logger = LoggerFactory.getLogger(RuleConfigRequestProcessor.class);


    public RuleConfigRequestProcessor() {

    }


    @Override
    public Message process(Object msg, ChannelHandlerContext ctx) {
        try {
            Message message = HttpRequestDispatcher.buildMessage((FullHttpRequest) msg, ctx, RuleConfigMessageBody.class);

            Channel client = ChannelSupervise.getClient();
            if (client == null){
                logger.error("RuleConfigRequestProcessor get client is null!");
                return null;
            }
            logger.debug("send ruleConfig event to client:" + client.id().asShortText());

            client.writeAndFlush(HttpUtils.response(message))
                    .addListener((ChannelFutureListener) future -> {
                        if (future.isSuccess()) {
                            logger.debug("send ruleConfig event success!");
                        } else {
                            logger.error("send ruleConfig event Error!", future.cause());
                        }
                    });
            return message;

        } catch (Exception e) {
            e.printStackTrace();
            logger.error("RuleConfigRequestProcessor error", e);
        }
        return null;
    }

    @Override
    public String path() {
        return Constant.RULE_CONFIG_REQUEST;
    }

}
