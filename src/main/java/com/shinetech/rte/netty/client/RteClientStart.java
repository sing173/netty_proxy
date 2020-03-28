package com.shinetech.rte.netty.client;

import com.alibaba.fastjson.JSONObject;
import com.shinetech.proxy.netty.common.Constant;
import com.shinetech.proxy.netty.common.buffer.RteClientResponseCache;
import com.google.common.collect.Maps;
import com.shinetech.proxy.netty.message.Message;
import io.netty.channel.ChannelFuture;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 测试连接rte服务端用
 */
public class RteClientStart {

    private static final Logger logger = LoggerFactory.getLogger(RteClientStart.class);


    public static RteClient buildClient(String host, int port, int clientThread, int heartIntervalMs, RteClientResponseCache cache) {

        RteClient client = RteClient.build(host, port, clientThread, heartIntervalMs);

        try {
            client.start();
            logger.info("Connect Server success, host:" + host + ",port:" + port);
        } catch (Exception e) {
            logger.error("Connect Server fail : " + e.getMessage());
        }


        return client;

    }


    public static void main(String[] args) {

        //测试用
        RteClientStart starter = new RteClientStart();
        RteClientResponseCache responseCache = RteClientResponseCache.newBuild();
        RteClient client =  RteClientStart.buildClient("127.0.0.1", 12900, 20, 15000, responseCache);



        String serialKey = starter.requestKey(); // 生成随机的序列
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("serialKey",serialKey);


        responseCache.createStub(serialKey);

        Map<String, String> headers = Maps.newHashMap();
        headers.put("serialKey", serialKey);

        ChannelFuture future = client.sendData(new Message(), Constant.DECISION_REQUEST);
        System.out.println(future);

        String response = responseCache.getResult(serialKey, 10000);


        System.out.println(response);
        logger.debug("返回：" + response);

    }

    private String requestKey() {
        return RandomStringUtils.randomAlphabetic(10);
    }

}
