package com.dataExtracting.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("dwd_dj_xxly_zf_detail")
public class ZfDetail {

    private String gridCode;
    private String gridName;
    private String shopName;
    private String shopCode;
    private Date visitTime;
    private Integer visitNum;
    private String processState;
    private Date jhptUpdateTime;
    private String dsjzxTaskid;
    private String sourceArea;
    private String areaName;
    private String streetCode;
    private String streetName;
    private String wgAreaCode;
    private String wgAreaName;
    private Date updateTime;

}
