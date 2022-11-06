package com.lkd.controller.http;

import com.google.common.base.Strings;
import com.lkd.common.VMSystem;
import com.lkd.config.ConsulConfig;
import com.lkd.exception.LogicException;
import com.lkd.feignService.OrderService;
import com.lkd.feignService.VMService;
import com.lkd.service.WxService;
import com.lkd.utils.DistributedLock;
import com.lkd.viewmodel.RequestPay;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

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

    @Autowired
    private ConsulConfig consulConfig;

    @Autowired
    private RedisTemplate<String,String> redisTemplate;

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
        //远程调用售货机服务检查商品库存
        if(!vmService.hasCapacity(requestPay.getInnerCode()
                ,Long.valueOf(requestPay.getSkuId()))){
            throw new LogicException("该商品已售空");
        }
        //如果openId为空，则根据jsCode生成
        if (Strings.isNullOrEmpty(requestPay.getOpenId())) {
            requestPay.setOpenId(wxService.getOpenId(requestPay.getJsCode()));
        }

        //分布式锁，机器同一时间只能处理一次出货
        DistributedLock lock = new DistributedLock(
                consulConfig.getConsulRegisterHost(),
                consulConfig.getConsulRegisterPort());
        //用机器编号做锁名称
        DistributedLock.LockContext lockContext = lock.getLock(requestPay.getInnerCode(),60);
        if(!lockContext.isGetLock()){
            throw new LogicException("机器出货中请稍后再试");
        }
        //存入redis后是为了取消订单时释放锁
        redisTemplate.boundValueOps(VMSystem.VM_LOCK_KEY_PREF+requestPay.getInnerCode())
                .set(lockContext.getSession(), Duration.ofSeconds(60));
        //远程调用订单微服务发起支付请求
        String responseData = orderService.requestPay(requestPay);
        if (Strings.isNullOrEmpty(responseData)) {
            throw new LogicException("微信支付接口调用失败");
        }
        return responseData;
    }
}
