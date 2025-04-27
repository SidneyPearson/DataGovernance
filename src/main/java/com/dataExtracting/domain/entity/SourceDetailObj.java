package com.dataExtracting.domain.entity;

import lombok.Data;

@Data
public class SourceDetailObj {
    private String gridCode;
    private String gridName;
    private String shopName;
    private String shopCode;
    private String visitTime;
    private String visitNum;
    private String processState;
    private String jhptUpdateTime;
    private String dsjzxTaskid;
}
