package com.shinetech.proxy.netty.message;

import java.util.Map;

/**
 * Created by luomingxing on 2020/3/17.
 */
public class DecisionMessageBody implements IMessageBody {

    private String dsType;
    private Map<String, Object> data;

    public DecisionMessageBody(){

    }

    @Override
    public String getDsType() {
        return dsType;
    }

    public void setDsType(String dsType) {
        this.dsType = dsType;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }
}
