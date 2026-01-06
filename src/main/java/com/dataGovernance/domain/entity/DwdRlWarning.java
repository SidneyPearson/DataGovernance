package com.dataGovernance.domain.entity;

import lombok.Data;
import java.util.Date;

@Data
public class DwdRlWarning {
    private Integer id;
    private Integer recordId;      // 对应表里的 record_id
    private String theme;
    private String warning;       // 对应表里的 warning (AI分析内容)
    private Integer status;        // 1异常 0正常
    private Date createTime;
}