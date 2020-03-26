package com.shinetech.common.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

/**
 * https://www.cnblogs.com/kzfy/p/6053568.html
 * 软负载 哈希算法
 */
@Deprecated
public class ConsistencyHash {
    public ConsistencyHash(List<Node> shards){
        shards = shards;
        init();
    }

    private static class Node{
        private String name;
        private String ip;

        public Node(String name, String ip) {
            this.name = name;
            this.ip = ip;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        @Override
        public String toString() {
            return "Node{" +
                    "ip='" + ip + '\'' +
                    ", name='" + name + '\'' +
                    '}';
        }
    }

    private static class Client{
        public Client(String name, Long hashCode) {
            this.name = name;
            this.hashCode = hashCode;
        }

        public Client(String name) {
            this.name = name;
        }

        private String name;
        private Long hashCode;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Long getHashCode() {
            return hashCode;
        }

        public void setHashCode(Long hashCode) {
            this.hashCode = hashCode;
        }
    }

    /**
     * 虚拟节点hash值 到 真实主机 的映射
     */
    private static TreeMap<Long,Node> nodes;

    /**
     * 客户端hash值 到 真实节点 的映射
     */
    private static TreeMap<Long,Node> treeKey;//

    /**
     * 真实主机
     */
    private static List<Node> shards = new ArrayList<Node>();

    /**
     * 客户端
     */
    private static List<Client> cliends = new ArrayList<Client>();

    /**
     * 客户端自己hash 和客户端的映射
     */
    private static TreeMap<Long,Client> clientTree;

    /**
     * 每个机器节点关联的虚拟节点个数
     */
    private static final int NODE_NUM = 100;


    private void init(){
        nodes = new TreeMap<Long, Node>();
        treeKey = new TreeMap<Long, Node>();
        clientTree = new TreeMap<Long, Client>();
        for (int i = 0; i < shards.size(); i++) {
            final Node shardInfo = shards.get(i);
            for (int n = 0; n < NODE_NUM; n++) {
                Long key = hash("SHARD-"+shardInfo.name+"-NODE-"+n);
                nodes.put(key,shardInfo);
            }
        }
    }

    /**
     * 添加一个真实主机
     */
    private void addNode(Node n){
        System.out.println("添加主机"+n+"的变化：");
        for (int i = 0; i < NODE_NUM; i++) {
            Long lg = hash("SHARD-"+n.name+"-NODE-"+i);
            SortedMap<Long,Node> head = nodes.headMap(lg);
            SortedMap<Long,Node> between;
            if(head.size() == 0){
                //建立在 最后一个虚拟主机的hash值 不和 客户端的hash值相等
                between = treeKey.tailMap(nodes.lastKey());
            }else{
                Long begin = head.lastKey();
                between = treeKey.subMap(begin,lg);
            }
            nodes.put(lg,n);
            for(Iterator<Long> it = between.keySet().iterator(); it.hasNext();){
                Long lo = it.next();
                treeKey.put(lo, nodes.get(lg));
            }
        }
    }

    /**
     * 删除一个真实主机
     * @param n
     */
    private void deleteNode(Node n){
        System.out.println("删除主机"+n+"的变化:");
        for (int i = 0; i < NODE_NUM; i++) {
            Long virturalHashCode = hash("SHARD-" + n.name + "-NODE-" + i);
            // 等于和大于 此值  == 顺时针 环形此值后面
            SortedMap<Long,Node> tail = nodes.tailMap(virturalHashCode);
            // 严格小于 此值(不等于) == 顺时针 环形此值前面
            SortedMap<Long,Node> head = nodes.headMap(virturalHashCode);
            SortedMap<Long, Node> between;
            if(head.size() == 0){
                between = treeKey.tailMap(nodes.lastKey());
            }else{
                Long begin = head.lastKey();
                Long end = tail.firstKey();
                /**
                 * 方法用于返回此映射的键值从fromKey(包括)到toKey(不包括)的部分视图。
                 * (如果fromKey和toKey相等，则返回映射为空)返回的映射受此映射支持，
                 * 因此改变返回映射反映在此映射中，反之亦然。
                 * 在n节点的第i个虚拟节点的所有key的集合
                 */
                between = treeKey.subMap(begin,end);
            }
            //从nodes中删除n节点的第i个虚拟节点
            nodes.remove(virturalHashCode);
            for(Iterator<Long> it = between.keySet().iterator();it.hasNext();){
                Long lo  = it.next();
                treeKey.put(lo, nodes.get(tail.firstKey()));
            }
        }
    }

    /**
     * 客户端hash值 映射 到 真实主机
     */
    private void keyToNode(List<Client> clients){
        for (Client client : clients) {
            Long hashCode = hash(client.getName());
            // 沿环的顺时针找到一个虚拟节点
            SortedMap<Long, Node> tail = nodes.tailMap(hashCode);
            Node node = tail.size() == 0 ? nodes.get(nodes.firstKey()) : nodes.get(tail.firstKey());
            treeKey.put(hashCode,node);
            client.setHashCode(hashCode);
            clientTree.put(hashCode,client);
        }
    }

    /**
     * 输出客户端 映射到 真实主机
     */
    private void printKeyTree(){
        for(Iterator<Long> it = treeKey.keySet().iterator();it.hasNext();){
            Long lo = it.next();
            System.out.println(clientTree.get(lo).name+"（hash："+lo+"）连接到主机->"+treeKey.get(lo));
        }
    }

    /**
     *  MurMurHash算法，是非加密HASH算法，性能很高，
     *  比传统的CRC32,MD5，SHA-1
     *  （这两个算法都是加密HASH算法，复杂度本身就很高，带来的性能上的损害也不可避免）
     *  等HASH算法要快很多，而且据说这个算法的碰撞率很低.
     *  http://murmurhash.googlepages.com/
     */
    private Long hash(String key){

        ByteBuffer buf = ByteBuffer.wrap(key.getBytes());
        int seed = 0x1234ABCD;

        ByteOrder byteOrder = buf.order();
        buf.order(ByteOrder.LITTLE_ENDIAN);

        long m = 0xc6a4a7935bd1e995L;
        int r = 47;

        long h = seed ^ (buf.remaining() * m);

        long k;
        while (buf.remaining() >= 8) {
            k = buf.getLong();

            k *= m;
            k ^= k >>> r;
            k *= m;

            h ^= k;
            h *= m;
        }

        if (buf.remaining() > 0) {
            ByteBuffer finish = ByteBuffer.allocate(8).order(
                    ByteOrder.LITTLE_ENDIAN);
            // for big-endian version, do this first:
            // finish.position(8-buf.remaining());
            finish.put(buf).rewind();
            h ^= finish.getLong();
            h *= m;
        }

        h ^= h >>> r;
        h *= m;
        h ^= h >>> r;

        buf.order(byteOrder);
        return h;
    }


    public static void main(String[] args) {
        /**
         * 客户端的hash值 和  真实主机的100个虚拟节点的hash值
         *
         * 一起均匀地分布在顺时针由小到大这个环上。(0 - 2^32 )
         *
         * 具体 客户端 最终 连接到 哪个主机，
         *
         * 原则是：将客户端hash值，顺时针往后 最近的 虚拟节点hash值。
         *
         */
        Node s1 = new Node("s1", "192.168.1.1");
        Node s2 = new Node("s2", "192.168.1.2");
        Node s3 = new Node("s3", "192.168.1.3");
        Node s4 = new Node("s4", "192.168.1.4");
        Node s5 = new Node("s5", "192.168.1.5");
        shards.add(s1);
        shards.add(s2);
        shards.add(s3);
        shards.add(s4);
        ConsistencyHash sh = new ConsistencyHash(shards);
        System.out.println("添加客户端，一开始有4个主机，分别为s1,s2,s3,s4,每个主机有100个虚拟主机：");
        for (int i = 1; i <= 9; i++) {
            String name = "10"+i+"客户端";
            cliends.add(new Client(name));
        }
        sh.keyToNode(cliends);
        sh.printKeyTree();
        sh.deleteNode(s2);
        sh.printKeyTree();
        sh.addNode(s5);
        sh.printKeyTree();
    }
}
