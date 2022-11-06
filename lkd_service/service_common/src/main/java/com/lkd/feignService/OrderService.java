package com.lkd.feignService;

import com.lkd.feignService.fallback.OrderServiceFallbackFactory;
import com.lkd.viewmodel.RequestPay;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(value = "order-service",fallbackFactory = OrderServiceFallbackFactory.class)
public interface OrderService {

    @GetMapping("/order/businessTop10Skus/{businessId}")
    List<Long> getBusinessTop10Skus(@PathVariable Integer businessId);

    @PostMapping("/wxpay/requestPay")
    String requestPay(@RequestBody RequestPay requestPay);

    @GetMapping("/order/cancel/{orderNo}")
    Boolean cancel(@PathVariable String orderNo);
}
