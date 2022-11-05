package com.lkd.controller.http;

import com.google.common.base.Strings;
import com.lkd.exception.LogicException;
import com.lkd.feignService.OrderService;
import com.lkd.feignService.VMService;
import com.lkd.service.WxService;
import com.lkd.viewmodel.RequestPay;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order")
@Slf4j
public class OrderController {


    @Autowired
    private WxService wxService;

    @Autowired
    private VMService vmService;

    @Autowired
    private OrderService orderService;

    /**
     * 获取openId
     *
     * @param jsCode
     * @return
     */
    @GetMapping("/openid/{jsCode}")
    public String getOpenid(@PathVariable String jsCode) {
        return wxService.getOpenId(jsCode);
    }

    /**
     * 小程序请求支付
     *
     * @param requestPay
     * @return
     */
    @PostMapping("/requestPay")
    public String requestPay(@RequestBody RequestPay requestPay) {
        //如果openId为空，则根据jsCode生成
        if (Strings.isNullOrEmpty(requestPay.getOpenId())) {
            requestPay.setOpenId(wxService.getOpenId(requestPay.getJsCode()));
        }
        //远程调用订单微服务发起支付请求
        String responseData = orderService.requestPay(requestPay);
        if (Strings.isNullOrEmpty(responseData)) {
            throw new LogicException("微信支付接口调用失败");
        }
        return responseData;
    }
}
