package com.shinetech.proxy.netty.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 本地客户端启动类
 * <p>
 * 1 需要指定服务端的ip和端口
 * <p>
 * 2 首次连接失败的重连机制
 */
public class LocalClientStart {

    private static final Logger logger = LoggerFactory.getLogger(LocalClientStart.class);
    private LocalClient client;

    private boolean startClient(String host, int port, int clientThread, int heartIntervalMs) {

        try {
            client = LocalClient.build(host, port, clientThread, heartIntervalMs);
            client.start();
            logger.info("Connect Server success, host:" + host + ",port:" + port);
        } catch (Exception e) {
            logger.error("Connect Server fail : " + e.getMessage());
        }


        return true;

    }

    public static void main(String[] args) {

//        Properties properties = PropertyUtils.getInstance();
//
//
//        String serverIp = (String) properties.get(Constant.SERVER_IP);
//        int port = Integer.parseInt((String) properties.get(Constant.SERVER_PORT));
//        //客户端接收请求的线程数
//        int nThreads = Integer.parseInt((String) properties.get(Constant.EVENT_LOOP_THREADS));
//        int beatInterval = Integer.parseInt((String) properties.get(Constant.HEARTBEAT_INTERVAL));

        String serverIp = "127.0.0.1";
        int port = 8889;
        //客户端接收请求的线程数
        int nThreads = 10;
        int beatInterval = 5000;


        logger.info("connect to " + serverIp + ":" + port + ", client loop threads:" + nThreads + ",heart beat interval " + beatInterval + "ms");

        LocalClientStart starter = new LocalClientStart();
        starter.startClient(serverIp, port, nThreads, beatInterval);

        Runtime.getRuntime().addShutdownHook(
                new Thread() {
                    @Override
                    public void run() {
                        //　先停掉
                        starter.client.disconnect();
                        logger.info("Client is shutdown.");
                    }
                }
        );

    }
}










