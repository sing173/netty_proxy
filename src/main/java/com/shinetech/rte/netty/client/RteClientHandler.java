package com.shinetech.rte.netty.client;

import com.alibaba.fastjson.JSON;
import com.shinetech.proxy.netty.common.buffer.ByteBufToBytes;
import com.shinetech.proxy.netty.message.Message;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;




/**
 * 客户端响应请求的内部处理程序
 */

public class RteClientHandler extends ChannelInboundHandlerAdapter {


    private static final Logger logger = LoggerFactory.getLogger(RteClientHandler.class);

    private RteClient client;

    private String host;
    private int port;


    public RteClientHandler(String host, int port, RteClient client) {
        this.host = host;
        this.port = port;
        this.client = client;

    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        logger.error("与rte服务端断开连接,尝试重连....................");
        client.doConnect();  //重新连接服务器
    }

    /**
     * 一段时间未进行读写操作 回调
     * @param ctx
     * @param evt
     * @throws Exception
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.ALL_IDLE) {
//                client.heartBeat();
            }
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    /**
     * 读取数据
     *
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {


        logger.debug("localClient Read:" + msg);

        try {
            if (msg instanceof FullHttpResponse){
                FullHttpResponse fullHttpResponse = (FullHttpResponse) msg;
                String jsonStr = fullHttpResponse.content().toString(HttpConstants.DEFAULT_CHARSET);
                Message message = JSON.parseObject(jsonStr, Message.class);


            } else {
                throw new Exception();
            }


        } catch (Exception e) {
            e.printStackTrace();
        }


    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }


}
