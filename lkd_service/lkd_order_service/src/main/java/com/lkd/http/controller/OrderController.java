package com.lkd.http.controller;

import com.lkd.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    /**
     * 取消订单
     * @param orderNo
     * @return
     */
    @GetMapping("/cancel/{orderNo}")
    public Boolean cancel(@PathVariable String orderNo){
        return orderService.cancel(orderNo);
    }
}
