package com.shinetech.proxy.netty.message;

import java.util.Map;

/**
 * Created by luomingxing on 2020/3/17.
 */
public class DecisionMessageBody implements IMessageBody {

    private String dsType;
    private Map<String, Object> eventVars;

    public DecisionMessageBody(){

    }

    @Override
    public String getDsType() {
        return dsType;
    }

    public void setDsType(String dsType) {
        this.dsType = dsType;
    }

    public Map<String, Object> getEventVars() {
        return eventVars;
    }

    public void setEventVars(Map<String, Object> eventVars) {
        this.eventVars = eventVars;
    }
}
