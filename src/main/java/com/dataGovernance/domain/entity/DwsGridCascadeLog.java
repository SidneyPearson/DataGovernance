package com.dataGovernance.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("dws_grid_cascade_log")
public class DwsGridCascadeLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskCode;
    private String taskName;
    private String taskType;
    private String dimCode;
    private String dimName;
    private String frequency;
    private String scheduleRule;
    private Long cascadeCount;
    private String cascadeStatus;
    private Date startTime;
    private Date endTime;
    private Long durationMs;
    private String errorMsg;
    private Date statDate;
    private Date createTime;
}
