package com.lkd.business.msgHandler;

import com.google.common.base.Strings;
import com.lkd.annotations.ProcessType;
import com.lkd.business.MsgHandler;
import com.lkd.contract.VendoutResp;
import com.lkd.service.VendingMachineService;
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
    private VendingMachineService vmService;
    @Override
    public void process(String jsonMsg) throws IOException {
        boolean success = JsonUtil.getNodeByName("success",jsonMsg).asBoolean();
        if (!success) return;
        VendoutResp vendoutResp = JsonUtil.getByJson(jsonMsg,VendoutResp.class);
        if(Strings.isNullOrEmpty(vendoutResp.getInnerCode())) return;

        vmService.vendOutResult(vendoutResp);
    }
}
