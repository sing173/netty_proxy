package com.shinetech.rte.netty.client;



import com.alibaba.fastjson.JSONObject;
import com.shinetech.proxy.netty.common.buffer.RteClientResponseCache;
import com.shinetech.proxy.netty.common.Constant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;


/**
 *   获取服务端返回的请求
 */
public class RiskClientResponseProcessor implements IRiskClientProcessor  {


    private static final Logger logger = LoggerFactory.getLogger(RiskClientResponseProcessor.class);

    private long TIMEOUT_MILLS = 50; // 决策超时时间 单位毫秒
    private RteClientResponseCache cache;



    public RiskClientResponseProcessor(RteClientResponseCache cache) {
        this.cache = cache;
    }


    @Override
    public Map<String, Object> process( String body) {

        JSONObject object = JSONObject.parseObject(body);
        cache.putResult((String) object.get(Constant.SERIAL_KEY), body);

        return null;
    }

    @Override
    public String path() {
        return "response";
    }


}
