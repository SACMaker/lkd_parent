package com.lkd.conf;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 读取微信支付配置文件
 */
@Component
@ConfigurationProperties("wxpay")
@Data
public class WXConfig {

    private String appId;
    private String appSecret;
    private String mchId;
    private String partnerKey;
    private String notifyUrl = "";

}
