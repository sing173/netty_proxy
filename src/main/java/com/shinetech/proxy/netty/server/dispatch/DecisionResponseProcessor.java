package com.shinetech.proxy.netty.server.dispatch;

import com.alibaba.fastjson.JSON;
import com.shinetech.proxy.netty.common.Constant;
import com.shinetech.proxy.netty.common.HttpUtils;
import com.shinetech.proxy.netty.common.IProcessor;
import com.shinetech.proxy.netty.server.ChannelSupervise;
import com.shinetech.proxy.netty.message.Message;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 *
 * Created by luomingxing on 2020/3/20.
 */
public class DecisionResponseProcessor implements IProcessor {
    private static final Logger logger = LoggerFactory.getLogger(DecisionResponseProcessor.class);


    @Override
    public Message process(Object msg, ChannelHandlerContext ctx) {
        FullHttpRequest request = (FullHttpRequest) msg;
        String jsonStr = request.content().toString(HttpConstants.DEFAULT_CHARSET);
        Message message = JSON.parseObject(jsonStr, Message.class);

        logger.debug("DecisionProcessor Response:" + message.getOutTime());

        Channel channel = ChannelSupervise.findChannel(message.getHeader().getRequestChannelId());

        if(channel != null){
            channel.writeAndFlush(HttpUtils.response(message)).addListener(ChannelFutureListener.CLOSE);
        } else {

        }

        return null;
    }

    @Override
    public String path() {
        return Constant.DECISION_RESPONSE;
    }
}
