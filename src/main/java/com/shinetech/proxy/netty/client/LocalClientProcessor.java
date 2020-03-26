package com.shinetech.proxy.netty.client;

import com.shinetech.common.utils.ZKClient;
import com.shinetech.proxy.netty.common.Constant;
import com.shinetech.proxy.netty.common.buffer.RteClientResponseCache;
import com.shinetech.rte.netty.client.RteClient;
import com.shinetech.rte.netty.client.RteClientStart;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *
 * @author luomingxing
 * @date 2020/3/26
 */
public class LocalClientProcessor implements Watcher {

    private static final Logger logger = LoggerFactory.getLogger(LocalClientProcessor.class);

    /**
     * 链接决策引擎的客户端集合
     */
    public ConcurrentMap<String, RteClient> rteClientPool = new ConcurrentHashMap<>();
    public List<String> keys = new ArrayList<>();
    private RteClientResponseCache responseCache;
    private String zkAddress;
    private ZKClient zkClient;


    public LocalClientProcessor(RteClientResponseCache responseCache) {
        try {

//            Properties properties = PropertyUtils.getInstance();
//            this.zkAddress = (String) properties.get(Constant.ZOOKEEPER_ADDRESS);

            this.responseCache = responseCache;
            this.zkAddress = Constant.ZOOKEEPER_ADDRESS;
            this.zkClient = new ZKClient(zkAddress, this);

            //不存在则新建, 这个目录必须存在
            if (!zkClient.exist(Constant.ZK_PROXY_CONFIG_PATH)) {
                zkClient.registerService(Constant.ZK_PROXY_CONFIG_PATH, "0");
                logger.info("create zk address：" + Constant.ZK_PROXY_CONFIG_PATH);
            }


        } catch (IOException e) {
            e.printStackTrace();
            logger.error("Zookeeper init error：" + e.getMessage());
        }

        //启动一个线程，处理zk连接断开的情况
        Runnable sendTask = new Runnable() {
            @Override
            public void run() {
                for (; ; ) {
                    logger.debug("check zookeeper Client  connection ......");
                    // 如果zk连接不上，重新初始化zk
                    if (!zkClient.isAvailable()) {
                        if (zkClient.reconnect(LocalClientProcessor.this)) {
                            initClientPool();
                        }
                    }
                    //休眠
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

    public void initClientPool() {
        // 带有事件监听，创建或者删除下一层节点，都会被通知到
        List<String> oldList = zkClient.listServices(Constant.FLINK_NETTY_SERVER, true);

        if (oldList != null && !oldList.isEmpty()) {
            logger.info("flink source netty server register info: " + oldList);

            ConcurrentMap<String, RteClient> tmpClientPool = new ConcurrentHashMap<>();
            List<String> tmpKeys = new ArrayList<>();

            for (String key : oldList) {
                String ip = "";
                String port = "";
                try {
                    ip = key.split(":")[0];
                    port = key.split(":")[1];
                } catch (Exception e) {
                    continue;//不是ip:port的格式，进入下一个循环
                }

                RteClient client = null;
                // 避免重复创建连接，只要ip和端口一样，会有15s重连机制
                if (rteClientPool.containsKey(key)) {
                    client = rteClientPool.get(key);
                } else {
                    client = RteClientStart.buildClient(ip, Integer.parseInt(port), 20, 15000, responseCache);
                }

                logger.info("add flink znode:" + ip + ":" + port);

                tmpClientPool.put(key, client);
                tmpKeys.add(key);
            }

            // 直接变量替换，避免直接操作的冲突
            rteClientPool = tmpClientPool;
            keys = tmpKeys;


        } else {
            logger.error("storm netty spout server is null in zk: " + zkAddress + " , path: " + Constant.FLINK_NETTY_SERVER);
        }
    }

    @Override
    public void process(WatchedEvent event) {

        String path = event.getPath();
        logger.info("zookeeper path has changed: " + path);

        if (Constant.ZK_PROXY_CONFIG_PATH.equalsIgnoreCase(path)) {
            initClientPool();
        }
    }
}
