package com.dataExtracting.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("dwd_dj_xxly_info_v2")
public class BaseInfoV2 {
    private String gridCode;
    private String gridName;
    private Integer sczt;
    private Integer cyry;
    private Integer fgsczt;
    private Integer jq;
    private Integer ly;
    private Integer yq;
    private Integer sq;
    private Integer sqsc;
    private Integer cun;
    private Integer qt;
    private Integer ddzjdzz;
    private Integer lhzjdzz;
    private Integer lydzz;
    private Integer yqdzz;
    private Integer jqdzz;
    private Integer sqscdzz;
    private Integer cdzz;
    private Integer hyzz;
    private Integer lsdzz;
    private Integer wfg;
    private Integer ddzjdzzry;
    private Integer lhzjdzzry;
    private Integer lydzzry;
    private Integer yqdzzry;
    private Integer jqdzzry;
    private Integer sqscdzzry;
    private Integer cdzzry;
    private Integer hyzzry;
    private Integer lsdzzry;
    private Integer wfgry;
    private Integer zgmyqy500qwc;
    private Integer zgmyqy500q;
    private Integer zghlwqy100qwc;
    private Integer zghlwqy100q;
    private Integer zjtxxjrqywc;
    private Integer zjtxxjrqy;
    private Integer jnzbssqywc;
    private Integer jnzbssqy;
    private Integer djsqywc;
    private Integer djsqy;
    private Integer kjxzxqywc;
    private Integer kjxzxqy;
    private Integer jymbfqydwwc;
    private Integer jymbfqydw;
    private Integer ylwsjgmbfqydwwc;
    private Integer ylwsjgmbfqydw;
    private Integer cpjrjgwc;
    private Integer cpjrjg;
    private Integer hlwqywc;
    private Integer hlwqy;
    private Integer dykjzxqywc;
    private Integer dykjzxqy;
    private String sourceArea;
    private String areaName;
    private String streetCode;
    private String streetName;
    private String wgAreaCode;
    private String wgAreaName;
    private String processState;
    private Date jhptUpdateTime;
    private Date updateTime;
    private String dsjzxTaskid;

}
