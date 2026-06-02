package com.dataGovernance.service;

import java.util.Map;

/**
 * 数据同步服务 - 填充 DWS 汇总表
 */
public interface DataSyncService {

    /**
     * 同步工单日指标（单日）
     */
    Map<String, Object> syncWorkorderDaily(String statDate);

    /**
     * 批量同步工单日指标
     */
    Map<String, Object> syncWorkorderDailyBatch(String startDate, String endDate);

    /**
     * 同步数据级联日志（mock）
     */
    Map<String, Object> syncCascadeLog(String statDate);

    /**
     * 批量同步数据级联日志
     */
    Map<String, Object> syncCascadeLogBatch(String startDate, String endDate);
}