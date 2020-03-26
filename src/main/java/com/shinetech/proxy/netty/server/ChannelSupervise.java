package com.shinetech.proxy.netty.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 本地客户端通道管理
 * @author luomingxing
 */
public class ChannelSupervise {
    private static ChannelGroup GlobalGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    public static ConcurrentMap<String, ChannelId> LocalChannelMap = new ConcurrentHashMap();
    /**
     * 本地客户端端channel
     */
    public static ConcurrentMap<String, ChannelId> ChannelMap = new ConcurrentHashMap();
    private static List<String> localChannelKeyIndex = new ArrayList<>();

    private static AtomicInteger roundRobin = new AtomicInteger(0);
    private static final int MAX_VALUE = 100000;
    private static final int MIN_VALUE = 0;

    public  static void addLocalChannel(Channel channel){
        GlobalGroup.add(channel);
        String key = channel.id().asShortText();
        LocalChannelMap.put(key,channel.id());
        localChannelKeyIndex.add(key);
    }

    public  static void addChannel(Channel channel){
        GlobalGroup.add(channel);
        String key = channel.id().asShortText();
        ChannelMap.put(key,channel.id());
    }

    public static void removeChannel(Channel channel){
        GlobalGroup.remove(channel);
        LocalChannelMap.remove(channel.id().asShortText());
        ChannelMap.remove(channel.id().asShortText());
        //TODO 删除key
    }


    public static  Channel findLocalChannel(String id){
        return GlobalGroup.find(LocalChannelMap.get(id));
    }

    public static  Channel findChannel(String id){
        return GlobalGroup.find(ChannelMap.get(id));
    }

    public static void send2All(TextWebSocketFrame tws){
        GlobalGroup.writeAndFlush(tws);
    }

    /**
     * 简单软负载 随机算法
     */
    public static Channel getClient() {
        if (LocalChannelMap == null || LocalChannelMap.size() == 0) {
            return null;
        }
        int index = getRoundRobinValue().get() % LocalChannelMap.size();
        return ChannelSupervise.findLocalChannel(localChannelKeyIndex.get(index));
    }

    private static AtomicInteger getRoundRobinValue() {
        if (roundRobin.getAndIncrement() > MAX_VALUE) {
            roundRobin.set(MIN_VALUE);
        }
        return roundRobin;
    }
}
