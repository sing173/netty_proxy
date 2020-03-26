package com.shinetech.rte.netty.client;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 客户端请求分发
 */
public class RiskClientDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(RiskClientDispatcher.class);

    //只用一个map就可以了
    private Map<String, IRiskClientProcessor> processorsMap = new HashMap();




    // 添加处理器 <path, processor>
    public void addProcessor(IRiskClientProcessor processor) {
        processorsMap.put(processor.path(), processor);

    }




    //分发请求 post
    public Map<String,Object>  dispatch(String body) {

        String path = path(body);
        if (processorsMap.containsKey(path)) {
            return processorsMap.get(path).process( body);
        }
        return null;


    }


    //获取请求的地址
    private String path(String body) {

        JSONObject object = JSONObject.parseObject(body);

        return object != null ? (String) object.get("type") : "" ;
    }


}
