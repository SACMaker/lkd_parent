package com.lkd.http.view;

import lombok.Data;

/**
 * 异常返回结果
 */
@Data
@Deprecated
public class ExceptionResponse{
    private String msg;
    public ExceptionResponse(String msg){
        this.msg = msg;
    }
}
