package com.dataGovernance.service;

import com.dataGovernance.domain.entity.AdsGridWorkorderRemark;
import com.dataGovernance.domain.entity.DwsGridCascadeLog;
import com.dataGovernance.domain.entity.DwsGridWorkorderDaily;

import java.util.List;
import java.util.Map;

/**
 * 数智网格数据运营监控 - 业务接口
 */
public interface GridOperationMonitorService {

    /** 数据级联：总量统计（库表/接口/文件分类 + 数据来源构成 + 同环比） */
    Map<String, Object> getCascadeOverview();

    /** 数据级联：实时数据列表 */
    List<Map<String, Object>> listCascadeLog(String dimCode, String taskType, Integer days);

    /** 数据级联：趋势（按日聚合） */
    List<Map<String, Object>> getCascadeTrend(Integer days);

    /** 数据级联：月度趋势（近 N 月） */
    List<Map<String, Object>> getCascadeMonthlyTrend(Integer months);

    /** 数据级联：手动新增一条级联记录（用于刷新按钮触发） */
    Long addCascadeLog(DwsGridCascadeLog log);

    /** 数据级联：单任务历史 */
    List<Map<String, Object>> listCascadeHistory(String taskCode);

    /** 工单分析：归集日历（按月） */
    List<Map<String, Object>> getWorkorderCalendar(String month);

    /** 工单分析：今日汇集工单总览 */
    Map<String, Object> getTodayWorkorderOverview(String areaCode);

    /** 工单分析：区域下拉 */
    List<Map<String, Object>> listWorkorderAreas();

    /** 工单分析：各区指标 */
    List<DwsGridWorkorderDaily> listDistrictIndicator(String statDate);

    /** 工单分析：汇集趋势 */
    List<Map<String, Object>> getWorkorderTrend(String areaCode, Integer days);

    /** 工单分析：周聚合趋势 */
    List<Map<String, Object>> getWorkorderTrendWeekly(String areaCode, Integer weeks);

    /** 工单分析：重点关注工单清单 */
    List<Map<String, Object>> listFocusWorkorder(String statDate, String srcType);

    /** 工单分析：清单展示 */
    List<Map<String, Object>> listWorkorder(String srcType, String areaCode, String startDate,
                                            String endDate, Integer pageSize);

    /** 工单清单备注：保存 */
    Long saveRemark(AdsGridWorkorderRemark remark);

    /** 工单清单备注：按工单查询 */
    List<AdsGridWorkorderRemark> listRemarkByTaskId(String taskId);
}
