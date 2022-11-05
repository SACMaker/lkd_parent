package com.lkd.controller.http;

import com.lkd.feignService.OrderService;
import com.lkd.feignService.VMService;
import com.lkd.service.WxService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
