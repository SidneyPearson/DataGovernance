package com.dataGovernance.controller;

import com.dataGovernance.domain.entity.AdsGridWorkorderRemark;
import com.dataGovernance.domain.entity.DwsGridCascadeLog;
import com.dataGovernance.domain.model.AjaxResult;
import com.dataGovernance.service.GridOperationMonitorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@Slf4j
@RestController
@RequestMapping("/grid/monitor")
public class GridOperationMonitorController extends BaseController {

    @Autowired
    private GridOperationMonitorService service;

    // ==================== 数据级联 ====================

    @GetMapping("/cascade/overview")
    public AjaxResult cascadeOverview() {
        long start = Instant.now().toEpochMilli();
        log.info("[API] GET /cascade/overview - 开始");
        Object data = service.getCascadeOverview();
        log.info("[API] GET /cascade/overview - 完成, 耗时={}ms", Instant.now().toEpochMilli() - start);
        return success(data);
    }

    @GetMapping("/cascade/list")
    public AjaxResult cascadeList(@RequestParam(required = false) String dimCode,
                                  @RequestParam(required = false) String taskType,
                                  @RequestParam(required = false, defaultValue = "20") Integer days) {
        long start = Instant.now().toEpochMilli();
        log.info("[API] GET /cascade/list - dimCode={}, taskType={}, days={}", dimCode, taskType, days);
        Object data = service.listCascadeLog(dimCode, taskType, days);
        log.info("[API] GET /cascade/list - 完成, 耗时={}ms", Instant.now().toEpochMilli() - start);
        return success(data);
    }

    @GetMapping("/cascade/trend")
    public AjaxResult cascadeTrend(@RequestParam(required = false, defaultValue = "7") Integer days) {
        long start = Instant.now().toEpochMilli();
        log.info("[API] GET /cascade/trend - days={}", days);
        Object data = service.getCascadeTrend(days);
        log.info("[API] GET /cascade/trend - 完成, 耗时={}ms", Instant.now().toEpochMilli() - start);
        return success(data);
    }

    @GetMapping("/cascade/trend/monthly")
    public AjaxResult cascadeMonthlyTrend(@RequestParam(required = false, defaultValue = "12") Integer months) {
        long start = Instant.now().toEpochMilli();
        log.info("[API] GET /cascade/trend/monthly - months={}", months);
        Object data = service.getCascadeMonthlyTrend(months);
        log.info("[API] GET /cascade/trend/monthly - 完成, 耗时={}ms", Instant.now().toEpochMilli() - start);
        return success(data);
    }

    @GetMapping("/cascade/history")
    public AjaxResult cascadeHistory(@RequestParam String taskCode) {
        long start = Instant.now().toEpochMilli();
        log.info("[API] GET /cascade/history - taskCode={}", taskCode);
        Object data = service.listCascadeHistory(taskCode);
        log.info("[API] GET /cascade/history - 完成, 耗时={}ms", Instant.now().toEpochMilli() - start);
        return success(data);
    }

    @PostMapping("/cascade/log")
    public AjaxResult addCascadeLog(@RequestBody DwsGridCascadeLog log) {
        logger.info("[API] POST /cascade/log - taskCode={}", log.getTaskCode());
        return success(service.addCascadeLog(log));
    }

    // ==================== 工单分析 ====================

    @GetMapping("/workorder/calendar")
    public AjaxResult workorderCalendar(@RequestParam(required = false) String month) {
        long start = Instant.now().toEpochMilli();
        log.info("[API] GET /workorder/calendar - month={}", month);
        Object data = service.getWorkorderCalendar(month);
        log.info("[API] GET /workorder/calendar - 完成, 耗时={}ms", Instant.now().toEpochMilli() - start);
        return success(data);
    }

    @GetMapping("/workorder/today")
    public AjaxResult workorderToday(@RequestParam(required = false) String areaCode) {
        long start = Instant.now().toEpochMilli();
        log.info("[API] GET /workorder/today - areaCode={}", areaCode);
        Object data = service.getTodayWorkorderOverview(areaCode);
        log.info("[API] GET /workorder/today - 完成, 耗时={}ms", Instant.now().toEpochMilli() - start);
        return success(data);
    }

    @GetMapping("/workorder/areas")
    public AjaxResult workorderAreas() {
        return success(service.listWorkorderAreas());
    }

    @GetMapping("/workorder/district")
    public AjaxResult workorderDistrict(@RequestParam(required = false) String statDate) {
        long start = Instant.now().toEpochMilli();
        log.info("[API] GET /workorder/district - statDate={}", statDate);
        Object data = service.listDistrictIndicator(statDate);
        log.info("[API] GET /workorder/district - 完成, 耗时={}ms", Instant.now().toEpochMilli() - start);
        return success(data);
    }

    @GetMapping("/workorder/trend")
    public AjaxResult workorderTrend(@RequestParam(required = false) String areaCode,
                                     @RequestParam(required = false, defaultValue = "7") Integer days) {
        long start = Instant.now().toEpochMilli();
        log.info("[API] GET /workorder/trend - areaCode={}, days={}", areaCode, days);
        Object data = service.getWorkorderTrend(areaCode, days);
        log.info("[API] GET /workorder/trend - 完成, 耗时={}ms", Instant.now().toEpochMilli() - start);
        return success(data);
    }

    @GetMapping("/workorder/trend/weekly")
    public AjaxResult workorderTrendWeekly(@RequestParam(required = false) String areaCode,
                                           @RequestParam(required = false, defaultValue = "8") Integer weeks) {
        long start = Instant.now().toEpochMilli();
        log.info("[API] GET /workorder/trend/weekly - areaCode={}, weeks={}", areaCode, weeks);
        Object data = service.getWorkorderTrendWeekly(areaCode, weeks);
        log.info("[API] GET /workorder/trend/weekly - 完成, 耗时={}ms", Instant.now().toEpochMilli() - start);
        return success(data);
    }

    @GetMapping("/workorder/focus")
    public AjaxResult workorderFocus(@RequestParam(required = false) String statDate,
                                     @RequestParam(required = false) String srcType) {
        long start = Instant.now().toEpochMilli();
        log.info("[API] GET /workorder/focus - statDate={}, srcType={}", statDate, srcType);
        Object data = service.listFocusWorkorder(statDate, srcType);
        log.info("[API] GET /workorder/focus - 完成, 耗时={}ms", Instant.now().toEpochMilli() - start);
        return success(data);
    }

    @GetMapping("/workorder/list")
    public AjaxResult workorderList(@RequestParam(required = false) String srcType,
                                    @RequestParam(required = false) String areaCode,
                                    @RequestParam(required = false) String startDate,
                                    @RequestParam(required = false) String endDate,
                                    @RequestParam(required = false, defaultValue = "20") Integer pageSize) {
        long start = Instant.now().toEpochMilli();
        log.info("[API] GET /workorder/list - srcType={}, areaCode={}, pageSize={}", srcType, areaCode, pageSize);
        Object data = service.listWorkorder(srcType, areaCode, startDate, endDate, pageSize);
        log.info("[API] GET /workorder/list - 完成, 耗时={}ms", Instant.now().toEpochMilli() - start);
        return success(data);
    }

    // ==================== 备注 ====================

    @PostMapping("/remark")
    public AjaxResult saveRemark(@RequestBody AdsGridWorkorderRemark remark) {
        log.info("[API] POST /remark - taskId={}", remark.getTaskId());
        return success(service.saveRemark(remark));
    }

    @GetMapping("/remark/{taskId}")
    public AjaxResult listRemark(@PathVariable String taskId) {
        log.info("[API] GET /remark/{}", taskId);
        return success(service.listRemarkByTaskId(taskId));
    }
}