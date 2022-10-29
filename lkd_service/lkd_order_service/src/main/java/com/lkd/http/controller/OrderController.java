package com.lkd.http.controller;

import com.lkd.service.OrderService;
import com.lkd.viewmodel.OrderViewModel;
import com.lkd.viewmodel.Pager;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
