package com.lkd.feignService.fallback;

import com.google.common.collect.Lists;
import com.lkd.feignService.OrderService;
import com.lkd.viewmodel.RequestPay;
import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class OrderServiceFallbackFactory implements FallbackFactory<OrderService> {
    @Override
    public OrderService create(Throwable throwable) {
        log.error("订单服务调用失败",throwable);
        return new OrderService() {

            @Override
            public List<Long> getBusinessTop10Skus(Integer businessId) {
                return Lists.newArrayList();
            }
            //请求支付失败回调方法
            @Override
            public String requestPay(RequestPay requestPay) {
                return null;
            }
        };
    }
}
