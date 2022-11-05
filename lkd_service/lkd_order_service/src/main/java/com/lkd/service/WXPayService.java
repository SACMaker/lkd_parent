package com.lkd.service;

/**
 * 微信支付接口
 */
public interface WXPayService {


    /**
     * 调用统一下单接口发起支付
     * @param orderNo
     * @return
     */
    String requestPay(String orderNo);


    /**
     * 微信回调之后的处理
     * @param notifyResult
     * @throws Exception
     */
    void notify(String notifyResult) throws Exception;

}
