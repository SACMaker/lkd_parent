package com.lkd.contract;

import lombok.Data;

import java.util.List;

/**
 * 售货机状态协议,
 * 机器直接用带群组的方式发送这个协议,然后工单服务接收
 */
@Data
public class VmStatusContract extends BaseContract{
    private List<StatusInfo> statusInfo;

    public VmStatusContract() {
        this.setMsgType("vmStatus");
    }
}
