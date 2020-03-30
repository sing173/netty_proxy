package com.shinetech.proxy.netty.server.dispatch;

import com.alibaba.fastjson.JSON;
import com.shinetech.proxy.netty.common.IProcessor;
import com.shinetech.proxy.netty.message.IMessageBody;
import com.shinetech.proxy.netty.message.Message;
import com.shinetech.proxy.netty.message.MessageHeader;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpConstants;
import io.netty.handler.codec.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 根据请求的参数和路径进行路由处理分发
 */
public class HttpRequestDispatcher {


    private static final Logger logger = LoggerFactory.getLogger(HttpRequestDispatcher.class);


    private Map<String, IProcessor> processorsMap = new HashMap();

    public HttpRequestDispatcher() {

    }


    /**
     * 添加处理器 <path, processor>
     * @param processor
     */
    public void addProcessor(IProcessor processor) {
        processorsMap.put(processor.path(), processor);
    }

    /**
     * 分发请求 post
     * 具体转发到那个处理，是从uri中提取
     * @return
     */
    public void dispatch(HttpRequest request, ChannelHandlerContext ctx) {
        String path = request.uri();

        if (processorsMap.containsKey(path)) {
            processorsMap.get(path).process(request, ctx);
        } else {
            logger.error("HttpRequestDispatcher UnKnow Request Path!");

        }
    }

    public static Message buildMessage(FullHttpRequest request, ChannelHandlerContext ctx, Class<? extends IMessageBody> bodyClass){
        //请求体json
        String jsonStr = request.content().toString(HttpConstants.DEFAULT_CHARSET);
        //记录请求路径和发起客户端
        MessageHeader messageHeader = new MessageHeader(request.uri(), ctx.channel().id().asShortText());
        //组装统一消息
        Message message = new Message<>(messageHeader, JSON.parseObject(jsonStr, bodyClass));
        message.setInTime(System.currentTimeMillis());
        return message;
    }


}
