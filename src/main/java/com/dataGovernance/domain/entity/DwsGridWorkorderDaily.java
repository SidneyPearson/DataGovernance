package com.dataGovernance.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("dws_grid_workorder_daily")
public class DwsGridWorkorderDaily {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Date statDate;
    private String areaCode;
    private String areaName;
    private Long totalCount;
    @TableField("src_12345_count")
    private Long src12345Count;
    private Long srcZjbCount;
    private Long newCount;
    @TableField("label_fill_count")
    private Long labelFillCount;
    @TableField("label_fill_rate")
    private BigDecimal labelFillRate;
    @TableField("label_code_count")
    private Long labelCodeCount;
    @TableField("label_code_rate")
    private BigDecimal labelCodeRate;
    @TableField("grid_in_count")
    private Long gridInCount;
    @TableField("grid_in_rate")
    private BigDecimal gridInRate;
    private Long focusCount;
    @TableField("city_ratio")
    private BigDecimal cityRatio;
    private Long changeCount;
    private String changeTrend;
    private String collectStatus;
    private Date collectTime;
    private String updateStatus;
    private Date updateTime;
    private Date createTime;
}
