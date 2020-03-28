package com.shinetech.proxy.netty.common;

public interface Constant {

    String HEART_BEAT = "/heartBeat";
    String DECISION_REQUEST = "/decision";
    String DECISION_RESPONSE = "/decision/resp";
    String RULE_CONFIG_REQUEST = "/rule";
    String RULE_CONFIG_RESPONSE = "/rule/resp";
    String LOCAL_CLIENT_CONNENT = "/client/connect";


    String SERVER_IP = "server.ip";
    String SERVER_PORT = "server.port";
    String HEARTBEAT_INTERVAL = "heartbeat.interval.ms";
    String EVENT_LOOP_THREADS = "event.loop.threads";

    String ALERT_TIMEOUT_MS = "alert.timeout.ms";

    String SERIAL_KEY = "serialKey";


//    String ZOOKEEPER_ADDRESS = "192.168.0.90:24002,192.168.0.91:24002,192.168.0.92:24002";
    String ZOOKEEPER_ADDRESS = "192.168.0.84:2181,192.168.0.85:2181,192.168.0.86:2181";

//    String ZOOKEEPER_ADDRESS = "zookeeper.address";
    String FLINK_NETTY_SERVER ="/flink_netty_path";





}

