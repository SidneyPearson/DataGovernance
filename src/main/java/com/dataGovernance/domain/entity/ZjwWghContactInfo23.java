package com.dataGovernance.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 市住建委（城市综合管理部）综合网格案件工单源表
 */
@Data
@TableName("zjw_wgh_t_contactinfo_23")
public class ZjwWghContactInfo23 {

    @TableId
    private String markUuid;

    private String taskid;
    private String infosourcename;
    private String infotypename;
    private String infoscname;
    private String areacode;
    private String areaname;
    private String streetcode;
    private String streetname;
    private String communitycode;
    private String communityname;
    private String gridcode;
    private String address;
    private BigDecimal longitudeWgs84;
    private BigDecimal latitudeWgs84;
    private String description;
    private String reporter;
    private String contactinfo;
    private Date discovertime;
    private Date accepttime;
    private Date dispatchtime;
    private Date solvingtime;
    private Date endtime;
    private BigDecimal alltime;
    private String nowstatus;
    private String nowstatusname;
    private String isoverdue;
    private String isdelay;
    private String isstandard;
    private String isend;
    private String caseend;
    private String acceptdeptcode;
    private String dispatchdeptcode;
    private Date markBusinessTime;
    private Date markUpdateTime;
}
