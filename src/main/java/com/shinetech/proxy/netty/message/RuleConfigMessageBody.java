package com.shinetech.proxy.netty.message;

/**
 * Created by luomingxing on 2020/3/17.
 */
public class RuleConfigMessageBody implements IMessageBody {

    private String dsType;
    private String rulePackId;

    public RuleConfigMessageBody(){

    }

    @Override
    public String getDsType() {
        return dsType;
    }

    public void setDsType(String dsType) {
        this.dsType = dsType;
    }

    public String getRulePackId() {
        return rulePackId;
    }

    public void setRulePackId(String rulePackId) {
        this.rulePackId = rulePackId;
    }
}
