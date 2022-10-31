package com.lkd.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 工单详情实体类
 */
@Data
@TableName(value = "tb_task_details")
public class TaskDetailsEntity implements Serializable{
    @TableId(value = "details_id",type = IdType.AUTO)
    private Long detailsId;//details_id
    @TableField(value = "task_id")
    private Long taskId;//工单Id
    @TableField(value = "channel_code")
    private String channelCode;//货道编号
    @TableField(value = "expect_capacity")
    private Integer expectCapacity;//补货数量
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
