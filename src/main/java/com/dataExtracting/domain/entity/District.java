package com.dataExtracting.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("dwd_wg_district")
public class District {

    private String code;

    private String name;

    private String sort;

    private String xxlyInfoSyn;

    private String xxlyZoufangSyn;


}
