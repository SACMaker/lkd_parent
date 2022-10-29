package com.lkd.http.view;

import lombok.Data;

/**
 * JWT令牌包装对象
 */
@Data
public class TokenObject{
    /**
     * 手机号
     */
    private String mobile;

    private Integer userId;

    /**
     * 登录类型 0：后台；1：运营运维端；2：合作商后台
     */
    private Integer loginType;
}
