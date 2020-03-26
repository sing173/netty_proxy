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


    String RISK_EVENT_ID = "risk.event.id";




    String ZOOKEEPER_ADDRESS = "zookeeper.address";
    String SPOUT_NETTY_SERVER="/spout_netty_path";
    String ZK_PROXY_CONFIG_PATH="/zk_proxy_config_path";



    String ASSEMBLE_JSON_ALERT = "alert";
    String JSON_FINAL_RESULT = "finalResult";



    String MYSQL_URL = "mysql.url";
    String MYSQL_USER = "mysql.user";
    String MYSQL_PASS = "mysql.password";



    String MYSQL_MGR_URL = "mysql.mgr.url";
    String MYSQL_MGR_USER = "mysql.mgr.user";
    String MYSQL_MGR_PASS = "mysql.mgr.password";

    String  NOTICE_SERVER = "notice.server";




}

