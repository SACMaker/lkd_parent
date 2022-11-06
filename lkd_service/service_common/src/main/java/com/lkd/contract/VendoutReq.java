package com.lkd.contract;

import lombok.Data;

/**
 * 出货请求的协议
 */
@Data
public class VendoutReq extends BaseContract{
    public VendoutReq() {
        this.setMsgType("vendoutReq");
    }

    private VendoutReqData vendoutData;
}
