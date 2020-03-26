package com.shinetech.proxy.netty.server;

import com.shinetech.proxy.netty.common.HttpUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.FullHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;


public class ServerStart {

    private static final Logger logger = LoggerFactory.getLogger(ServerStart.class);

    private HttpServer server;

    private boolean startServer(int port) {

        try {

            server = HttpServer.build(port);
            server.start();

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        }

        return true;

    }

    private void stop() {
        if (server != null) {
            server.stop();
        }
    }

    private void scheduleSend() {

        Runnable sendTask = new Runnable() {

            @Override
            public void run() {
                sendTaskLoop:
                for (; ; ) {
                    try {
                        Iterator<String> it = ChannelSupervise.LocalChannelMap.keySet().iterator();
                        while (it.hasNext()) {
                            String key = it.next();
                            Channel obj = ChannelSupervise.findLocalChannel(key);

                            if(obj.isOpen()){
                                logger.debug("scheduleSend:" + obj);
                                // 模拟下发数据
                                FullHttpResponse response = HttpUtils.response();
                                obj.writeAndFlush(response).addListener(new ChannelFutureListener() {
                                    @Override
                                    public void operationComplete(ChannelFuture future) throws Exception {
                                        if (future.isSuccess()) {
                                            logger.debug("scheduleSend success");
                                        } else {
                                            throw new Exception();
                                        }

                                    }
                                });
                            }



                        }
                    } catch (Exception e) {
                        break sendTaskLoop;
                    }

                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };


        new Thread(sendTask).start();
    }

    public static void main(String[] args) {
//        Properties properties = PropertyUtils.getInstance();
        ServerStart starter = new ServerStart();
//        starter.startServer(Integer.parseInt((String) properties.get(Constant.SERVER_PORT)));
        starter.startServer(8889);
//        starter.scheduleSend();

        //关闭
        Runtime.getRuntime().addShutdownHook(
                new Thread() {
                    @Override
                    public void run() {
                        //　先停掉
                        starter.stop();
                        logger.info("Server is shutdown.");
                    }
                }
        );
    }


}
