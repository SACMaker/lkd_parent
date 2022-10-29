package com.lkd.viewmodel;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户工作量
 */
@Data
public class UserWork implements Serializable {
    /**
     * 用户Id
     */
    private Integer userId;
    /**
     * 用户名
     */
    private String userName;

    /**
     * 用户角色名
     */
    private String roleName;

    /**
     * 完成工单数
     */
    private Integer workCount;

    /**
     * 当日进行中的工单
     */
    private Integer progressTotal;

    /**
     * 拒绝工单数
     */
    private Integer cancelCount;
}
