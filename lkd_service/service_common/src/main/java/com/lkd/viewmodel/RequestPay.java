package com.lkd.viewmodel;

import lombok.Data;

import java.io.Serializable;

/**
 * 小程序端支付请求对象-VO
 */
@Data
public class RequestPay implements Serializable {
    /**
     * 售货机编号
     */
    private String innerCode;

    /**
     * 小程序端JsCode
     */
    private String jsCode;

    /**
     * openId
     */
    private String openId;

    /**
     * 商品Id
     */
    private String skuId;
}