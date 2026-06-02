package com.dataGovernance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dataGovernance.domain.entity.AdsGridWorkorderRemark;
import com.dataGovernance.domain.entity.DwsGridCascadeLog;
import com.dataGovernance.domain.entity.DwsGridWorkorderDaily;
import com.dataGovernance.mapper.AdsGridWorkorderRemarkMapper;
import com.dataGovernance.mapper.BaseInfoMapper;
import com.dataGovernance.mapper.DwsGridCascadeLogMapper;
import com.dataGovernance.mapper.DwsGridWorkorderDailyMapper;
import com.dataGovernance.mapper.ZjwWghContactInfo23Mapper;
import com.dataGovernance.service.GridOperationMonitorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
public class GridOperationMonitorServiceImpl implements GridOperationMonitorService {

    @Autowired
    private DwsGridCascadeLogMapper cascadeLogMapper;

    @Autowired
    private DwsGridWorkorderDailyMapper workorderDailyMapper;

    @Autowired
    private AdsGridWorkorderRemarkMapper remarkMapper;

    @Autowired
    private BaseInfoMapper rxb12345Mapper;

    @Autowired
    private ZjwWghContactInfo23Mapper zjwMapper;

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    // ==================== 数据级联 ====================

    @Override
    public Map<String, Object> getCascadeOverview() {
        Map<String, Object> data = new LinkedHashMap<>();

        Long today = nz(cascadeLogMapper.sumToday());
        Long yesterday = nz(cascadeLogMapper.sumYesterday());
        Long all = nz(cascadeLogMapper.sumAll());

        data.put("todayTotal", today);
        data.put("historyTotal", all);
        data.put("changeCount", today - yesterday);
        data.put("changePercent", percent(today - yesterday, yesterday));

        // 类型分布
        Map<String, Object> byType = new LinkedHashMap<>();
        byType.put("TABLE", 0L); byType.put("API", 0L); byType.put("FILE", 0L);
        for (Map<String, Object> row : cascadeLogMapper.sumByType()) {
            String key = String.valueOf(row.get("taskType"));
            byType.put(key, asLong(row.get("cnt")));
        }

        // 分类型本月数量
        Map<String, Long> byTypeMonth = new LinkedHashMap<>();
        byTypeMonth.put("TABLE", 0L); byTypeMonth.put("API", 0L); byTypeMonth.put("FILE", 0L);
        for (Map<String, Object> row : cascadeLogMapper.sumByTypeMonth()) {
            String key = String.valueOf(row.get("taskType"));
            byTypeMonth.put(key, asLong(row.get("cnt")));
        }

        // 分类型上月数量
        Map<String, Long> byTypePrevMonth = new LinkedHashMap<>();
        byTypePrevMonth.put("TABLE", 0L); byTypePrevMonth.put("API", 0L); byTypePrevMonth.put("FILE", 0L);
        for (Map<String, Object> row : cascadeLogMapper.sumByTypePrevMonth()) {
            String key = String.valueOf(row.get("taskType"));
            byTypePrevMonth.put(key, asLong(row.get("cnt")));
        }

        // 增长% = 当月级联数 / 历史总量，保留2位小数
        long total = byType.values().stream().mapToLong(v -> ((Number) v).longValue()).sum();
        for (String key : new String[]{"TABLE", "API", "FILE"}) {
            long month = byTypeMonth.get(key);
            long prev = byTypePrevMonth.get(key);
            byType.put(key + "Change", month - prev);
            byType.put(key + "ChangePct", total > 0 ? Math.round(month * 10000.0 / total) / 100.0 : 0.0);
        }

        data.put("byType", byType);
        data.put("byDim", cascadeLogMapper.sumByDim());
        data.put("refreshTime", new Date());
        return data;
    }

    @Override
    public List<Map<String, Object>> listCascadeLog(String dimCode, String taskType, Integer days) {
        return cascadeLogMapper.listLatestPerTask(emptyToNull(dimCode), emptyToNull(taskType));
    }

    @Override
    public List<Map<String, Object>> getCascadeTrend(Integer days) {
        int n = days == null || days <= 0 ? 7 : days;
        List<Map<String, Object>> raw = cascadeLogMapper.trendByDay(n);

        // 透视：每个 statDate 一行，列为 TABLE/API/FILE
        Map<String, Map<String, Object>> bucket = new LinkedHashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, -n + 1);
        for (int i = 0; i < n; i++) {
            String d = sdf.format(c.getTime());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("statDate", d);
            row.put("table", 0L);
            row.put("api", 0L);
            row.put("file", 0L);
            bucket.put(d, row);
            c.add(Calendar.DAY_OF_MONTH, 1);
        }
        for (Map<String, Object> r : raw) {
            String d = String.valueOf(r.get("statDate"));
            String t = String.valueOf(r.get("taskType"));
            long v = asLong(r.get("cnt"));
            Map<String, Object> row = bucket.get(d);
            if (row == null) continue;
            if ("TABLE".equals(t)) row.put("table", v);
            else if ("API".equals(t)) row.put("api", v);
            else if ("FILE".equals(t)) row.put("file", v);
        }

        // 最后一天用近4日总量代替
        String lastDate = sdf.format(Calendar.getInstance().getTime());
        Map<String, Object> lastRow = bucket.get(lastDate);
        if (lastRow != null) {
            for (Map<String, Object> r : cascadeLogMapper.trendLast4DaysByType()) {
                String t = String.valueOf(r.get("taskType"));
                long v = asLong(r.get("cnt"));
                if ("TABLE".equals(t)) lastRow.put("table", v);
                else if ("API".equals(t)) lastRow.put("api", v);
                else if ("FILE".equals(t)) lastRow.put("file", v);
            }
        }

        return new ArrayList<>(bucket.values());
    }

    @Override
    public List<Map<String, Object>> getCascadeMonthlyTrend(Integer months) {
        int n = months == null || months <= 0 ? 12 : months;
        List<Map<String, Object>> raw = cascadeLogMapper.trendByMonth(n);

        // 补齐缺失月份
        Map<String, Long> fill = new LinkedHashMap<>();
        Calendar c = Calendar.getInstance();
        c.add(Calendar.MONTH, -n + 1);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM");
        for (int i = 0; i < n; i++) {
            fill.put(sdf.format(c.getTime()), 0L);
            c.add(Calendar.MONTH, 1);
        }
        for (Map<String, Object> r : raw) {
            fill.put(String.valueOf(r.get("month")), asLong(r.get("cnt")));
        }
        List<Map<String, Object>> result = new ArrayList<>();
        fill.forEach((k, v) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("month", k);
            row.put("value", v);
            result.add(row);
        });
        return result;
    }

    @Override
    public Long addCascadeLog(DwsGridCascadeLog log) {
        if (log.getStatDate() == null) log.setStatDate(new Date());
        if (log.getCreateTime() == null) log.setCreateTime(new Date());
        if (log.getCascadeStatus() == null) log.setCascadeStatus("SUCCESS");
        cascadeLogMapper.insert(log);
        return log.getId();
    }

    @Override
    public List<Map<String, Object>> listCascadeHistory(String taskCode) {
        if (taskCode == null || taskCode.isEmpty()) return Collections.emptyList();
        return cascadeLogMapper.listHistoryByTask(taskCode);
    }

    // ==================== 工单分析 ====================

    @Override
    public List<Map<String, Object>> getWorkorderCalendar(String month) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM");
        String m = month != null && month.matches("\\d{4}-\\d{2}") ? month : sdf.format(new Date());
        long t0 = Instant.now().toEpochMilli();
        List<Map<String, Object>> rows = workorderDailyMapper.selectCalendar(m);
        log.info("[S] getWorkorderCalendar, month={}, resultSize={}, cost={}ms", m, rows != null ? rows.size() : 0, Instant.now().toEpochMilli() - t0);
        if (rows != null && !rows.isEmpty()) return rows;
        // 回退：根据 12345 + 住建委源表按日聚合
        return calendarFromSources(m);
    }

    private List<Map<String, Object>> calendarFromSources(String month) {
        SimpleDateFormat ymd = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        String[] ym = month.split("-");
        c.set(Integer.parseInt(ym[0]), Integer.parseInt(ym[1]) - 1, 1);
        int days = c.getActualMaximum(Calendar.DAY_OF_MONTH);
        Date today = new Date();
        List<Map<String, Object>> result = new ArrayList<>(days);
        for (int d = 1; d <= days; d++) {
            c.set(Calendar.DAY_OF_MONTH, d);
            Map<String, Object> row = new LinkedHashMap<>();
            String key = ymd.format(c.getTime());
            row.put("statDate", key);
            row.put("collectStatus", c.getTime().after(today) ? "UNDONE" : "DONE");
            row.put("collectCount", 0);
            row.put("collectTime", "");
            result.add(row);
        }
        return result;
    }

    @Override
    public Map<String, Object> getTodayWorkorderOverview(String areaCode) {
        Map<String, Object> data = new LinkedHashMap<>();

        String twoDaysAgoDate = dateOffset(-2);
        String threeDaysAgoDate = dateOffset(-3);

        // 前2日数据（替代原今日）
        DwsGridWorkorderDaily twoDaysAgo = (areaCode == null || areaCode.isEmpty() || "all".equals(areaCode))
                ? workorderDailyMapper.selectCity2DaysAgo()
                : workorderDailyMapper.selectByAreaAndDate(areaCode, twoDaysAgoDate);

        // 前3日数据（用于对比）
        DwsGridWorkorderDaily threeDaysAgo = (areaCode == null || areaCode.isEmpty() || "all".equals(areaCode))
                ? workorderDailyMapper.selectCity3DaysAgo()
                : workorderDailyMapper.selectByAreaAndDate(areaCode, threeDaysAgoDate);

        if (twoDaysAgo != null) {
            data.put("total", nz(twoDaysAgo.getTotalCount()));
            data.put("src12345", nz(twoDaysAgo.getSrc12345Count()));
            data.put("srcZjb", nz(twoDaysAgo.getSrcZjbCount()));
            data.put("focusCount", nz(twoDaysAgo.getFocusCount()));
            data.put("labelFillRate", twoDaysAgo.getLabelFillRate() == null ? BigDecimal.ZERO : twoDaysAgo.getLabelFillRate());
            data.put("labelCodeRate", twoDaysAgo.getLabelCodeRate() == null ? BigDecimal.ZERO : twoDaysAgo.getLabelCodeRate());
            data.put("gridInRate", twoDaysAgo.getGridInRate() == null ? BigDecimal.ZERO : twoDaysAgo.getGridInRate());
            data.put("cityRatio", twoDaysAgo.getCityRatio() == null ? BigDecimal.ZERO : twoDaysAgo.getCityRatio());
        } else {
            // 回退：直接对源表实时聚合，口径也使用前2日
            long c12345 = nz(rxb12345Mapper.countByDate(twoDaysAgoDate));
            long czjb = nz(zjwMapper.countByDate(twoDaysAgoDate));
            long total = c12345 + czjb;
            data.put("total", total);
            data.put("src12345", c12345);
            data.put("srcZjb", czjb);
            data.put("focusCount", BigDecimal.ZERO);
            data.put("labelFillRate", BigDecimal.ZERO);
            data.put("labelCodeRate", BigDecimal.ZERO);
            data.put("gridInRate", BigDecimal.ZERO);
            data.put("cityRatio", new BigDecimal("100"));
        }

        // 较前3日变化
        long todayTotal = asLong(data.get("total"));
        long yesterdayTotal = threeDaysAgo == null ? 0L : nz(threeDaysAgo.getTotalCount());
        data.put("changeCount", todayTotal - yesterdayTotal);
        data.put("changePercent", percent(todayTotal - yesterdayTotal, yesterdayTotal));

        // 历史累计总量
        long historyTotal = (areaCode == null || areaCode.isEmpty() || "all".equals(areaCode))
                ? nz(workorderDailyMapper.sumCityAll())
                : nz(workorderDailyMapper.sumAreaAll(areaCode));
        data.put("historyTotal", historyTotal);

        // 工单类型分布：当前区域历史累计按类型求和
        data.put("typeDistribution", buildTypeDistribution(areaCode));

        data.put("refreshTime", new Date());
        return data;
    }

    private Map<String, Object> buildTypeDistribution(String areaCode) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> rows = workorderDailyMapper.selectTypeDistribution(emptyToNull(areaCode));
        log.info("[S] selectTypeDistribution result={}", rows);
        long total = 0;
        for (Map<String, Object> r : rows) {
            String type = String.valueOf(r.get("src_type"));
            long cnt = asLong(r.get("cnt"));
            result.put(type, cnt);
            total += cnt;
        }
        log.info("[S] typeDistribution total={}", total);
        // 计算占比
        if (total > 0) {
            result.put("total", total);
            long src12345 = asLong(result.get("12345"));
            long srcZjb = asLong(result.get("ZJB"));
            result.put("12345Pct", Math.round(src12345 * 10000.0 / total) / 100.0);
            result.put("ZJBPct", Math.round(srcZjb * 10000.0 / total) / 100.0);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> listWorkorderAreas() {
        return workorderDailyMapper.selectAreaOptions();
    }

    @Override
    public List<DwsGridWorkorderDaily> listDistrictIndicator(String statDate) {
        List<DwsGridWorkorderDaily> rows = workorderDailyMapper.selectDistrictsAll();
        if (rows != null && !rows.isEmpty()) return rows;
        // 回退：直接 zjw 源表 + 12345 源表 合并区县历史计数
        return districtFallback(null);
    }

    private List<DwsGridWorkorderDaily> districtFallback(String statDate) {
        long t0 = Instant.now().toEpochMilli();
        Map<String, DwsGridWorkorderDaily> byCode = new LinkedHashMap<>();

        log.info("[S] 开始获取住建委源表数据");
        for (Map<String, Object> r : zjwMapper.countByArea(statDate)) {
            String code = String.valueOf(r.get("areaCode"));
            DwsGridWorkorderDaily d = byCode.computeIfAbsent(code, k -> {
                DwsGridWorkorderDaily x = new DwsGridWorkorderDaily();
                x.setAreaCode(code);
                x.setAreaName(String.valueOf(r.get("areaName")));
                x.setSrc12345Count(0L);
                x.setSrcZjbCount(0L);
                x.setTotalCount(0L);
                return x;
            });
            d.setSrcZjbCount(asLong(r.get("cnt")));
            d.setTotalCount(nz(d.getTotalCount()) + nz(d.getSrcZjbCount()));
        }
        log.info("[S] 住建委源表获取完成, byCode.size={}, cost={}ms", byCode.size(), Instant.now().toEpochMilli() - t0);

        t0 = Instant.now().toEpochMilli();
        log.info("[S] 开始获取12345源表数据");
        // 12345 数据用 areaName 匹配，加到对应区
        for (Map<String, Object> r : rxb12345Mapper.countByDistrict(statDate)) {
            String name = String.valueOf(r.get("areaName"));
            for (DwsGridWorkorderDaily d : byCode.values()) {
                if (name != null && name.equals(d.getAreaName())) {
                    d.setSrc12345Count(asLong(r.get("cnt")));
                    d.setTotalCount(nz(d.getTotalCount()) + asLong(r.get("cnt")));
                    break;
                }
            }
        }
        log.info("[S] 12345源表获取完成, cost={}ms", Instant.now().toEpochMilli() - t0);

        long total = byCode.values().stream().mapToLong(d -> nz(d.getTotalCount())).sum();
        for (DwsGridWorkorderDaily d : byCode.values()) {
            d.setCollectStatus("DONE");
            d.setUpdateStatus("UPDATED");
            d.setCollectTime(new Date());
            d.setUpdateTime(new Date());
            d.setCityRatio(total == 0 ? BigDecimal.ZERO :
                    new BigDecimal(nz(d.getTotalCount())).divide(new BigDecimal(total), 4, RoundingMode.HALF_UP).multiply(HUNDRED));
        }
        List<DwsGridWorkorderDaily> list = new ArrayList<>(byCode.values());
        list.sort((a, b) -> Long.compare(nz(b.getTotalCount()), nz(a.getTotalCount())));
        return list;
    }

    @Override
    public List<Map<String, Object>> getWorkorderTrend(String areaCode, Integer days) {
        int n = days == null || days <= 0 ? 7 : days;
        long t0 = Instant.now().toEpochMilli();
        List<Map<String, Object>> rows = workorderDailyMapper.selectTrend(areaCode, n);
        log.info("[S] getWorkorderTrend, areaCode={}, days={}, resultSize={}, cost={}ms", areaCode, n, rows != null ? rows.size() : 0, Instant.now().toEpochMilli() - t0);
        if (rows != null && !rows.isEmpty()) return rows;
        return Collections.emptyList();
    }

    @Override
    public List<Map<String, Object>> getWorkorderTrendWeekly(String areaCode, Integer weeks) {
        int n = weeks == null || weeks <= 0 ? 8 : weeks;
        return workorderDailyMapper.selectTrendByWeek(areaCode, n);
    }

    @Override
    public List<Map<String, Object>> listFocusWorkorder(String statDate, String srcType) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (srcType == null || srcType.isEmpty() || "all".equals(srcType) || "ZJB".equals(srcType)) {
            for (Map<String, Object> r : zjwMapper.selectFocus(statDate)) {
                r.put("srcType", "ZJB");
                result.add(r);
            }
        }
        if (srcType == null || srcType.isEmpty() || "all".equals(srcType) || "12345".equals(srcType)) {
            for (Map<String, Object> r : rxb12345Mapper.selectFocusList(statDate)) {
                r.put("srcType", "12345");
                result.add(r);
            }
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> listWorkorder(String srcType, String areaCode,
                                                   String startDate, String endDate, Integer pageSize) {
        int size = pageSize == null || pageSize <= 0 ? 20 : pageSize;
        List<Map<String, Object>> result = new ArrayList<>();
        if (srcType == null || srcType.isEmpty() || "all".equals(srcType) || "ZJB".equals(srcType)) {
            for (Map<String, Object> r : zjwMapper.selectList(areaCode, startDate, endDate, size)) {
                r.put("srcType", "ZJB");
                r.put("wpType", r.get("infoTypeName"));
                r.put("wpSource", r.get("infoSourceName"));
                result.add(r);
            }
        }
        if (srcType == null || srcType.isEmpty() || "all".equals(srcType) || "12345".equals(srcType)) {
            // 12345 表的 areaCode 字段是 rel_district 中文名，前端传过来的若是数字 code 这里就略过
            String district = areaCode;
            if (district != null && district.matches("\\d+")) district = null;
            for (Map<String, Object> r : rxb12345Mapper.selectWorkorderList(district, startDate, endDate, size)) {
                r.put("srcType", "12345");
                result.add(r);
            }
        }
        return result;
    }

    @Override
    public Long saveRemark(AdsGridWorkorderRemark remark) {
        Date now = new Date();
        if (remark.getId() == null) {
            if (remark.getCreateTime() == null) remark.setCreateTime(now);
            if (remark.getDelFlag() == null) remark.setDelFlag(0);
            remarkMapper.insert(remark);
        } else {
            remark.setUpdateTime(now);
            remarkMapper.updateById(remark);
        }
        return remark.getId();
    }

    @Override
    public List<AdsGridWorkorderRemark> listRemarkByTaskId(String taskId) {
        LambdaQueryWrapper<AdsGridWorkorderRemark> qw = new LambdaQueryWrapper<>();
        qw.eq(AdsGridWorkorderRemark::getTaskId, taskId)
                .eq(AdsGridWorkorderRemark::getDelFlag, 0)
                .orderByDesc(AdsGridWorkorderRemark::getCreateTime);
        return remarkMapper.selectList(qw);
    }

    // ==================== 工具方法 ====================

    private static long nz(Long v) { return v == null ? 0L : v; }

    private static long asLong(Object v) {
        if (v == null) return 0L;
        if (v instanceof Number) return ((Number) v).longValue();
        try { return Long.parseLong(String.valueOf(v)); } catch (Exception e) { return 0L; }
    }

    private static String emptyToNull(String s) { return s == null || s.isEmpty() || "all".equals(s) ? null : s; }

    private static String dateOffset(int days) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, days);
        return new SimpleDateFormat("yyyy-MM-dd").format(c.getTime());
    }

    private static BigDecimal percent(long delta, long base) {
        if (base == 0) return BigDecimal.ZERO;
        return new BigDecimal(delta).multiply(HUNDRED).divide(new BigDecimal(base), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal rate(Map<String, Object> row, String num, String den) {
        if (row == null) return BigDecimal.ZERO;
        long n = asLong(row.get(num));
        long d = asLong(row.get(den));
        if (d == 0) return BigDecimal.ZERO;
        return new BigDecimal(n).multiply(HUNDRED).divide(new BigDecimal(d), 2, RoundingMode.HALF_UP);
    }
}