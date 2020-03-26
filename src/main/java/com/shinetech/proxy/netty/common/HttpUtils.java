package com.shinetech.proxy.netty.common;

import com.alibaba.fastjson.JSON;
import com.shinetech.proxy.netty.message.Message;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import io.netty.util.AsciiString;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * Created by luomingxing on 2020/3/20.
 */
public class HttpUtils {


    private static final AsciiString CONTENT_TYPE = AsciiString.of("Content-Type");
    private static final AsciiString CONTENT_LENGTH = AsciiString.of("Content-Length");
//    private static final AsciiString CONNECTION = AsciiString.of("Connection");
//    private static final AsciiString KEEP_ALIVE = AsciiString.of("keep-alive");

    private static String host = "127.0.0.1";

    public static DefaultFullHttpRequest request(Message data, String url) throws UnsupportedEncodingException, URISyntaxException {

        DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST,
                url, Unpooled.wrappedBuffer(JSON.toJSONString(data).getBytes("UTF-8")));


        request.headers().set(HttpHeaderNames.HOST, host);
        request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        request.headers().set(HttpHeaderNames.CACHE_CONTROL, HttpHeaderValues.NO_CACHE);
        request.headers().set(HttpHeaderNames.CONTENT_TYPE,  "application/json; charset=UTF-8");
        request.headers().set(HttpHeaderNames.CONTENT_LENGTH, request.content().readableBytes());
        return request;
    }

    public static DefaultFullHttpResponse response(){
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK);
        response.headers().set(CONTENT_TYPE, "text/plain");
        response.headers().setInt(CONTENT_LENGTH, response.content().readableBytes());

        return response;
    }

    public static DefaultFullHttpResponse response(Object message){
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK);
        response.content().writeBytes(JSON.toJSONBytes(message));

        response.headers().set(CONTENT_TYPE, "text/plain");
        response.headers().setInt(CONTENT_LENGTH, response.content().readableBytes());


        return response;
    }
}
