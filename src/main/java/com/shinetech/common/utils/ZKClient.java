package com.shinetech.common.utils;

import org.apache.commons.collections.CollectionUtils;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;


public class ZKClient {

    private ZooKeeper zkClient;
    private CreateMode mode;
    private String zkAddress;
    private static final Logger logger = LoggerFactory.getLogger(ZKClient.class);

    public ZKClient(String string, Watcher watcher) throws IOException {
        this(string, CreateMode.PERSISTENT, watcher);
    }

    public ZKClient(String string, CreateMode mode , Watcher watcher) throws IOException {
        this.zkClient = new ZooKeeper(string, 30000, watcher);
        this.mode = mode;
        zkAddress = string;
    }

    public boolean reconnect(Watcher watcher) {
        try {
            zkClient.close();
        } catch (InterruptedException e) {
        }
        try {
            zkClient = new ZooKeeper(zkAddress, 30000, watcher);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean isAvailable() {
        ZooKeeper.States state = zkClient.getState();
        if (state != null) {
            return state.isConnected() && state.isAlive();
        }
        return false;
    }

    public void close() throws InterruptedException {
        this.zkClient.close();
    }

    public String registerService(String path, String data) {
        try {
            return this.zkClient.create(path, data.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, mode);
        } catch (KeeperException var4) {
            logger.error(var4.getMessage());
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
        }
        return null;
    }

    public boolean updateRegisterService(String path, String data) {
        try {
            this.zkClient.setData(path, data.getBytes(), -1);
            return true;
        } catch (KeeperException var4) {
            logger.error(var4.getMessage());
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
        }
        return false;
    }

    public boolean exist(String path) {
        try {
            Stat stat = this.zkClient.exists(path, false);
            if (stat == null) {
                return false;
            } else {
                return true;
            }
        } catch (KeeperException var4) {
            logger.error(var4.getMessage());
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
        }
        return false;
    }

    public void unregisterServiceTree(String path){
        List<String> l = listServices(path);
        if(CollectionUtils.isNotEmpty(l)){
            for(String lp:l){
                unregisterServiceTree(path + "/" + lp);
            }
            for(String lp:l){
                unregisterService(path+"/"+lp);
            }
        }


    }

    public void unregisterService(String path) {
        try {
            this.zkClient.delete(path, -1);
        } catch (KeeperException var3) {
            logger.error(var3.getMessage());
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
        }
    }

    public List<String> listServices(String path, boolean watch) {
        List children = Collections.emptyList();
        try {
            children = this.zkClient.getChildren(path, watch);
        } catch (KeeperException var4) {
            logger.error(var4.getMessage());
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
        }catch (Exception e){
            logger.error(e.getMessage(),e);
        }
        return children;
    }


    public List<String> listServices(String path) {
        return listServices(path,false);
    }

    public String getServiceData(String path, boolean watch) {
        try {
            Stat ke = new Stat();
            byte[] byteData = this.zkClient.getData(path, watch, ke);
            String data = new String(byteData);
            return data;
        } catch (KeeperException var5) {
            logger.error(var5.getMessage());
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
        }
        return null;
    }



    public static void main(String[]  args) {

//        try {
//            String zkAddress = "ZKF1.S0001.WJ-KF-B.BJ.JRX:2181,ZKF2.S0002.WJ-KF-B.BJ.JRX:2181,ZKF3.S0003.WJ-KF-B.BJ.JRX:2181";
//
//            ZKClient zkClient = new ZKClient(zkAddress);
//            List<String> oldList = zkClient.listServices("/spout_netty_path");
//            if (oldList != null && !oldList.isEmpty()) {
//                logger.info("remove old netty spout server register info: " + oldList);
//                System.out.println(oldList);
//            } else {
//                logger.info("netty spout server is null in zk: " + zkAddress + " , path: " + "/spout_netty_path");
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }
}
