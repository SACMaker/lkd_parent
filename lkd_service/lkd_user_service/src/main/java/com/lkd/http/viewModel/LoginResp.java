package com.lkd.http.viewModel;

import lombok.Data;

/**
 * 封装响应类-VO
 */
@Data
public class LoginResp{
    private long userId;
    private String userName;
    private String roleCode;
    private String token; //jwt令牌
    private boolean success;
    private String regionId;
    private String msg;
    /**
     * 是否是运维人员
     */
    private boolean isRepair;
}
