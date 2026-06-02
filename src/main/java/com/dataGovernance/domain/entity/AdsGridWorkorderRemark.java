package com.dataGovernance.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("ads_grid_workorder_remark")
public class AdsGridWorkorderRemark {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskId;
    private String srcType;
    private String targetType;
    private String targetDesc;
    private String remarkContent;
    private Date statDate;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;
    private Integer delFlag;
}
