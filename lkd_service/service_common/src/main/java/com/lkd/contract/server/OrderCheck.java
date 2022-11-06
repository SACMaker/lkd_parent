package com.lkd.contract.server;
import com.lkd.contract.AbstractContract;
import lombok.Data;

import java.io.Serializable;

/**
 * 订单检查协议
 */
@Data
public class OrderCheck extends AbstractContract implements Serializable {
    public OrderCheck() {
        this.setMsgType("orderCheck");
    }
    private String orderNo;
}