package com.dataExtracting.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 新兴领域党组织数据表实体类
 */
@Data
@TableName("dwd_dj_xxly_dzz_detail")
public class DzzDetail {

    /**
     * 综合网格编码，例：ZH329881123
     */
    private String wgCode;

    /**
     * 综合网格名称
     */
    private String wgName;

    /**
     * 党组织编码
     */
    private String orgCode;

    /**
     * 党组织名称
     */
    private String orgName;

    /**
     * 组织类型
     */
    private String orgType;

    /**
     * 新兴领域党组织所在区
     */
    private String communityName;

    /**
     * 新兴领域党组织类型
     */
    private String emergingFiledType;

    /**
     * 单位性质类别
     */
    private String unitType;

    /**
     * 更新时间-数据入前置库的时间
     */
    private Date dzzlxUpdateTime;

    /**
     * 数据批次号-数据入前置库日期
     */
    private String dzzlxTaskid;

    /**
     * 更新时间
     */
    private Date updateTime;

//    private String areaCode;
    private String areaName;


}