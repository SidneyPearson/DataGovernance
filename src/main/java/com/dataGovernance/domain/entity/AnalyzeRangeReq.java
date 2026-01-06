package com.dataGovernance.domain.entity;

import lombok.Data;

import java.util.List;

@Data
public class AnalyzeRangeReq {

    private String startTime; // yyyy-MM-dd HH:mm:ss
    private String endTime;

    private List<String> gridCodes;
}
