package com.lkd.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 售货机在es中存储的信息
 */
@Data
public class VmInfoDTO implements Serializable {
    /**
     * 售货机经纬度信息
     */
    private String location;
    /**
     * 售货机编号
     */
    private String innerCode;
    /**
     * 点位名称
     */
    private String nodeName;
    /**
     * 详细地址
     */
    private String addr;
    /**
     * 距离(单位：米)
     */
    private Integer distance;
    /**
     * 售货机类型
     */
    private String typeName;
}
