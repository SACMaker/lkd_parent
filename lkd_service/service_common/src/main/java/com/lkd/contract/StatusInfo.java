package com.lkd.contract;

import lombok.Data;

import java.io.Serializable;

/**
 * 设备状态信息-DTO
 */
@Data
public class StatusInfo implements Serializable{
    /**
     * 状态码
     */
    private String statusCode;
    /**
     * 是否正常
     */
    private boolean status;
}
