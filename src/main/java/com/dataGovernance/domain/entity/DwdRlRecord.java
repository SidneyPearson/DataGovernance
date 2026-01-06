package com.dataGovernance.domain.entity;

import lombok.Data;

@Data
public class DwdRlRecord {

    private Integer id;
    private String gridCode;
    private String gridName;
    private String street;
    private String area;

    private Long beginTime;
    private Long endTime;

    private Integer population;
}
