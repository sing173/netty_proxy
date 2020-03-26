package com.shinetech.proxy.netty.message;


import com.alibaba.fastjson.JSON;

import java.io.Serializable;

/**
 *  报文组成：
 *  报文头、报文域
 *
 * @author luomingxing
 */
public class Message<T extends IMessageBody> implements Serializable {
    /**
     * 报文头
     */
    private MessageHeader  header;
    /**
     * 报文域
     */
    private T body;

    private long inTime;
    private long outTime;

    public Message() {
        //心跳的创建方式
    }

    public Message(MessageHeader header, T body) {
        this.header = header;
        this.body = body;
    }


    public MessageHeader getHeader() {
        return header;
    }


    public T getBody() {
        return body;
    }

    public void setHeader(MessageHeader header) {
        this.header = header;
    }

    public void setBody(T body) {
        this.body = body;
    }

    @Override
    public String toString() {
        if(this.header == null ) {
            return "heart beat!";
        }

        return "header: " + JSON.toJSONString(header) +
                "\n body:" + JSON.toJSONString(body);
    }

    public long getInTime() {
        return inTime;
    }

    public void setInTime(long inTime) {
        this.inTime = inTime;
    }

    public long getOutTime() {
        return outTime;
    }

    public void setOutTime(long outTime) {
        this.outTime = outTime;
    }
}
