package com.shinetech.proxy.netty.message;

import java.io.Serializable;

/**
 *  报文体的接口类
 */
public interface IMessageBody extends Serializable{
    String getDsType();

}
