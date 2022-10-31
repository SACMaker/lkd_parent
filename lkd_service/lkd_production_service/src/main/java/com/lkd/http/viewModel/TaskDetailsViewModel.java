package com.lkd.http.viewModel;

import lombok.Data;

/**
 * 工单详情VO
 */
@Data
public class TaskDetailsViewModel{
    /**
     * 货道编号
     */
    private String channelCode;
    /**
     * 期望数量
     */
    private int expectCapacity;
    /**
     * 商品Id
     */
    private Long skuId;
    /**
     * 商品名称
     */
    private String skuName;
    /**
     * 商品图片
     */
    private String skuImage;
}
