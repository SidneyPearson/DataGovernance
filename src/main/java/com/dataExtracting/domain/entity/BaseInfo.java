package com.dataExtracting.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("dwd_dj_xxly_info")
public class BaseInfo {
    private String gridCode;
    private String gridName;
    private Integer sczt;
    private Integer cyry;
    private Integer zfll;
    private Integer jldzz;
    private Integer qzdw;
    private Integer qzdzz;
    private Integer qzdzb;
    private Integer fgqy;
    private Integer syfgqy;
    private Integer hhsyzqy;
    private Integer gtgsh;
    private Integer sqjjhmf;
    private Integer shzjzz;
    private Integer mbfqydw;
    private Integer lssws;
    private Integer kjssws;
    private Integer swssws;
    private Integer zcpgjg;
    private Integer jymbfqydw;
    private Integer ylwsjgmbfqydw;
    private Integer cpjrjg;
    private Integer dfjrjg;
    private Integer jq;
    private Integer ly;
    private Integer yq;
    private Integer sqsc;
    private Integer cun;
    private Integer dwfg;
    private Integer ddzjdzz;
    private Integer lhzjdzz;
    private Integer qyfg;
    private Integer lydzz;
    private Integer yqdzz;
    private Integer jqdzz;
    private Integer sqscdzz;
    private Integer cdzz;
    private Integer hyfg;
    private Integer wfg;
    private Integer nmsczts;
    private Integer tyssczts;
    private String processState;
    private String sourceArea;
    private String areaName;
    private String streetCode;
    private String streetName;
    private Date jhptUpdateTime;
    private Date updateTime;
    private String dsjzxTaskid;
    private String wgAreaCode;
    private String wgAreaName;

}
