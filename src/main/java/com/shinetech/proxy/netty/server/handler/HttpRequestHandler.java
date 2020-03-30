package com.shinetech.proxy.netty.server.handler;

import com.shinetech.proxy.netty.common.Constant;
import com.shinetech.proxy.netty.server.ChannelSupervise;
import com.shinetech.proxy.netty.server.dispatch.HttpRequestDispatcher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * http服务端事件处理器
 *
 * @author luomingxing
 * @date 2020/3/13
 */
public class HttpRequestHandler extends ChannelInboundHandlerAdapter {
    private final Logger logger = LoggerFactory.getLogger(HttpRequestHandler.class);

    private static final String FAVICON_ICO = "/favicon.ico";

    private HttpRequestDispatcher dispatcher;

    public HttpRequestHandler(HttpRequestDispatcher dispatcher){
        this.dispatcher = dispatcher;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.debug("new client connect："+ctx.channel());
        ChannelSupervise.addChannel(ctx.channel());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.debug("client disconnect："+ctx.channel());
        ChannelSupervise.removeChannel(ctx.channel());
        super.channelInactive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        logger.debug("HttpRequestHandler channelRead");
        try {
            if (msg instanceof HttpRequest){
                HttpRequest request = (HttpRequest) msg;
                HttpHeaders headers = request.headers();
                String uri = request.uri();

                logger.info("http request uri: "+ uri);
                if (uri.equals(FAVICON_ICO)){
                    return;
                }

                HttpMethod method = request.method();
                if (method.equals(HttpMethod.POST)){
                    String contentType = getContentType(headers);
                    //只处理json的请求
                    if("application/json".equals(contentType)){
                        //为了区分上游客户端和本地客户端，本地客户端链接服务端后会发一条注册信息过来，服务端保存channel
                        if(Constant.LOCAL_CLIENT_CONNENT.equals(uri)) {
                            logger.debug("new local client connect："+ctx.channel());
                            ChannelSupervise.addLocalChannel(ctx.channel());
                            //删除channelActive加入的连接，避免重复
                            ChannelSupervise.ChannelMap.remove(ctx.channel().id().asShortText());
                        } else {
                            //其它请求通过分发器处理
                            dispatcher.dispatch(request, ctx);
                        }
                    }
                }
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    private String getContentType(HttpHeaders headers){
        String typeStr = headers.get("Content-Type");
        String[] list = typeStr.split(";");
        return list[0];
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("HttpRequestHandler Error！", cause);
        ctx.close();
    }
}
