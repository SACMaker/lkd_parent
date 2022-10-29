package com.lkd.sms;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 短信服务配置类,用于进行配置绑定
 */
@Configuration
public class SmsConfig {
    @Value("${sms.operator.signName}")
    private String signName;
    @Value("${sms.operator.templateCode}")
    private String templateCode;

    public String getSignName() {
        return signName;
    }

    public String getTemplateCode() {
        return templateCode;
    }
}
