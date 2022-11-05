package com.lkd.viewmodel;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建订单-VO
 */
@Data
public class CreateOrder implements Serializable {
    /**
     * 售货机编号
     */
    private String innerCode;
    /**
     * 用户openId
     */
    private String openId;
    /**
     * 商品Id
     */
    private String skuId;

    /**
     * 支付方式
     */
    private String payType;
}
