package com.lkd.feignService;

import com.lkd.feignService.fallback.OrderServiceFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(value = "order-service",fallbackFactory = OrderServiceFallbackFactory.class)
public interface OrderService {

    @GetMapping("/order/businessTop10Skus/{businessId}")
    List<Long> getBusinessTop10Skus(@PathVariable Integer businessId);
}
