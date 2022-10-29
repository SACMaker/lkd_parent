package com.lkd.business;

import com.lkd.annotations.ProcessType;
import com.lkd.common.VMSystem;
import com.lkd.config.ConsulConfig;
import com.lkd.contract.VendoutResp;
import com.lkd.redis.RedisUtils;
import com.lkd.service.OrderService;
import com.lkd.utils.DistributedLock;
import com.lkd.utils.JsonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 处理出货结果
 */
@Component
@ProcessType(value = "vendoutResp")
public class VendoutMsgHandler implements MsgHandler{
    @Autowired
    private ConsulConfig consulConfig;
    @Autowired
    private RedisUtils redisUtils;
    @Autowired
    private OrderService orderService;
    @Override
    public void process(String jsonMsg) throws IOException {
        VendoutResp vendoutResp = JsonUtil.getByJson(jsonMsg, VendoutResp.class);
        //解锁售货机状态
        DistributedLock lock = new DistributedLock(
                consulConfig.getConsulRegisterHost(),
                consulConfig.getConsulRegisterPort());
        String sessionId = redisUtils.get(VMSystem.VM_LOCK_KEY_PREF+vendoutResp.getInnerCode(),String.class);
        lock.releaseLock(sessionId);

        //处理出货结果
        orderService.vendoutResult(vendoutResp);
    }
}
