package com.shinetech.proxy.netty.message;

import java.io.Serializable;
import java.util.UUID;

/**
 * 报文头
 */
public class MessageHeader implements Serializable{


    private String path;
    private String msgNo;
    private String requestChannelId;

    public MessageHeader(){

    }

    public MessageHeader(String path) {
        this.path = path;

    }

    public MessageHeader(String path, String requestChannelId) {
        this.path = path;
        this.requestChannelId = requestChannelId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getMsgNo() {
        return msgNo;
    }

    public void setMsgNo(String msgNo) {
        this.msgNo = msgNo;
    }

    public String getRequestChannelId() {
        return requestChannelId;
    }

    public void setRequestChannelId(String requestChannelId) {
        this.requestChannelId = requestChannelId;
    }
}
