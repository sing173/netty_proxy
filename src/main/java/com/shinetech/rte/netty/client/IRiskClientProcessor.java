package com.shinetech.rte.netty.client;


import java.util.Map;


/**
 * 客户端对事件的处理
 */
public interface IRiskClientProcessor {


    /**
     * 处理拿到的tcp请求
     *
     * @return 返回的response 返回结果的map
     */
    Map<String,Object> process(String body);

    /**
     * 路径
     *
     * @return
     */
    String path();

}
