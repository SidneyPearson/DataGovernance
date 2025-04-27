package com.dataExtracting.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("全市综合网格")
public class CityGrid {

    @TableField("objectid")
    private Long objectid;

    @TableField("网格编码")
    private String gridCode;

    @TableField("所属街道")
    private String streetName;

    @TableField("所属区县")
    private String districtName;

    private String areaCode;

    private String streetCode;
}
