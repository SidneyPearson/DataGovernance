package com.dataGovernance.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.sql.Timestamp;
import java.util.Date;

/**
 * 表：dsjzx.dwd_aj_wgaj_info_1208
 */
@Data
@TableName("dwd_aj_wgaj_info_1208")
public class DwdAjWgajInfo1208 {
    private String verifyImg;
    private String reportImg;
    private String contactPhone;
    private String reporter;
    private String caseType;
    private String issueSrc;
    private String problemDescription;
    private String yCoordinate;
    private String taskId;
    private String gridCode;
    private String streetName;
    private String vallageCommunity;
    private String discoverTime;
    private String caseStatus;
    private String settleTime;
    private String occurrenceAddress;
    private String xCoordinate;
    private Date inserttime;
    private Date updatetime;
    private String areaCode;
    private String area;
    private String streetCode;
    private String gridName;
    private String street;
    private Date discoverTimeTs;
    private String sourceArea;
    private Date mergeTime;
    private Integer ql;
    private Integer qw;
    private Integer zd;
    private String wpid;
    private String wpType;
    private String discoverTimeYm;

    /** 网格字段 */
    private String 网格编码;
    private String 所属街道;
    private String 所属区县;
    private String 网格名称;
    private String gridAllName;
    private String areaCodeNew;
    private String streetCodeNew;
}
