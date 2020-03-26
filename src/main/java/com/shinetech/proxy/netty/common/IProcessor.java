package com.shinetech.proxy.netty.common;

import com.shinetech.proxy.netty.message.Message;
import io.netty.channel.ChannelHandlerContext;

/**
 * netty中的处理类
 */
public interface IProcessor {

    Message process(Object msg, ChannelHandlerContext ctx);

    String path();

}
