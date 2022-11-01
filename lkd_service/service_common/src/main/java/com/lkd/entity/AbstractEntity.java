package com.lkd.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.lkd.mybatis.GeneralMetaObjectHandler;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * create_time&update_time的父类
 * 抽取出create_time&update_time给mybatisplus填充字段
 * @see GeneralMetaObjectHandler
 */
@Data
public class AbstractEntity implements Serializable {
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    protected LocalDateTime createTime;
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    protected LocalDateTime updateTime;
}
