package com.lkd.contract;

import lombok.Data;

import java.io.Serializable;
/**
 * 协议父类
 */
@Data
public abstract class AbstractContract implements Serializable {
    /**
     * 消息类型
     */
    protected String msgType;
}
