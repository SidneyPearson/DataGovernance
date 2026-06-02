package com.dataGovernance.controller;

import com.dataGovernance.domain.model.AjaxResult;
import com.dataGovernance.service.DataSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/grid/sync")
public class DataSyncController {

    @Autowired
    private DataSyncService dataSyncService;

    /**
     * 同步工单日指标（单日）
     * POST /DataGovernance/grid/sync/workorder/daily
     */
    @PostMapping("/workorder/daily")
    public AjaxResult syncWorkorderDaily(@RequestBody(required = false) Map<String, String> params) {
        String statDate = params != null ? params.get("statDate") : null;
        log.info("同步工单日指标请求: statDate={}", statDate);
        Map<String, Object> result = dataSyncService.syncWorkorderDaily(statDate);
        log.info("同步工单日指标结果: {}", result);
        return AjaxResult.success(result);
    }

    /**
     * 批量同步工单日指标
     * POST /DataGovernance/grid/sync/workorder/daily/batch
     */
    @PostMapping("/workorder/daily/batch")
    public AjaxResult syncWorkorderDailyBatch(@RequestBody Map<String, String> params) {
        String startDate = params.get("startDate");
        String endDate = params.get("endDate");
        log.info("批量同步工单日指标: startDate={}, endDate={}", startDate, endDate);
        if (startDate == null || endDate == null) {
            return AjaxResult.error("startDate 和 endDate 不能为空");
        }
        Map<String, Object> result = dataSyncService.syncWorkorderDailyBatch(startDate, endDate);
        log.info("批量同步工单日指标结果: {}", result);
        return AjaxResult.success(result);
    }

    /**
     * 同步数据级联日志（mock）
     * POST /DataGovernance/grid/sync/cascade/log
     */
    @PostMapping("/cascade/log")
    public AjaxResult syncCascadeLog(@RequestBody(required = false) Map<String, String> params) {
        String statDate = params != null ? params.get("statDate") : null;
        log.info("同步级联日志请求: statDate={}", statDate);
        Map<String, Object> result = dataSyncService.syncCascadeLog(statDate);
        log.info("同步级联日志结果: {}", result);
        return AjaxResult.success(result);
    }

    /**
     * 批量同步数据级联日志
     * POST /DataGovernance/grid/sync/cascade/log/batch
     */
    @PostMapping("/cascade/log/batch")
    public AjaxResult syncCascadeLogBatch(@RequestBody(required = false) Map<String, String> params) {
        String startDate = params != null ? params.get("startDate") : null;
        String endDate = params != null ? params.get("endDate") : null;
        log.info("批量同步级联日志: startDate={}, endDate={}", startDate, endDate);
        if (startDate == null || endDate == null) {
            return AjaxResult.error("startDate 和 endDate 不能为空");
        }
        Map<String, Object> result = dataSyncService.syncCascadeLogBatch(startDate, endDate);
        log.info("批量同步级联日志结果: {}", result);
        return AjaxResult.success(result);
    }
}