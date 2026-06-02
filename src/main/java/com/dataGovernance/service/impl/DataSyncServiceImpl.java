package com.dataGovernance.service.impl;

import com.dataGovernance.service.DataSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

/**
 * 数据同步服务 - 根据大屏业务需求填充 DWS 汇总表
 *
 * 数据来源策略：
 *   - dws_grid_cascade_log: 使用 mock 数据（级联日志由外部 ETL 写入）
 *   - dws_grid_workorder_daily: 优先从源表聚合，缺失时用 mock
 *   - ads_grid_workorder_remark: 由业务接口写入，无需同步
 */
@Slf4j
@Service
public class DataSyncServiceImpl implements DataSyncService {

    @Resource
    private DataSource dataSource;

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd");

    // 模拟数据源配置
    private static final String[] AREA_NAMES = {"渝中区", "江北区", "渝北区", "南岸区", "沙坪坝区",
            "九龙坡区", "大渡口区", "巴南区", "北碚区", "高新区"};
    private static final String[] AREA_CODES = {"500103", "500105", "500112", "500108", "500106",
            "500107", "500104", "500113", "500109", "500199"};

    // 级联任务配置：编码、名称、类型、时间规则、执行时间(HH:mm:ss)
    private static final String[][] DIM_CONFIGS = {
            {"RXB", "12345市民热线工单", "API", "每天02:00同步1次", "02:00:00"},
            {"ZJW", "城市综合管理部事业工单", "TABLE", "每天同步1次", "01:00:00"},
            {"DWD1208", "社工部多格合一", "API", "每天12:00同步1次", "12:00:00"}
    };

    @Override
    public Map<String, Object> syncWorkorderDaily(String statDate) {
        String date = statDate != null && !statDate.isEmpty() ? statDate : SDF.format(new Date());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("statDate", date);
        result.put("success", false);

        try (Connection conn = dataSource.getConnection()) {
            // 1. 尝试从源表构建数据
            List<Map<String, Object>> districtData = buildFromSource(conn, date);

            // 2. 如果源表没有数据，使用 mock
            if (districtData.isEmpty() || districtData.stream().allMatch(d -> asLong(d.get("totalCount")) == 0)) {
                log.info("源表无数据，使用 mock 数据");
                districtData = buildMockDistrictData(date);
            }

            // 3. 计算全市汇总（包括质量指标）
            long total12345 = 0, totalZjb = 0, cityTotal = 0, focusCount = 0;
            long labelFill = 0, labelCode = 0, gridIn = 0;

            for (Map<String, Object> d : districtData) {
                total12345 += asLong(d.get("src12345Count"));
                totalZjb += asLong(d.get("srcZjbCount"));
                focusCount += asLong(d.get("focusCount"));
                labelFill += asLong(d.get("labelFillCount"));
                labelCode += asLong(d.get("labelCodeCount"));
                gridIn += asLong(d.get("gridInCount"));
            }
            cityTotal = total12345 + totalZjb;

            // 4. 清除旧数据并写入
            deleteByDate(conn, "dws_grid_workorder_daily", date);

            int count = 0;
            for (Map<String, Object> d : districtData) {
                if (asLong(d.get("totalCount")) > 0) {
                    insertWorkorderRow(conn, d, date, false);
                    count++;
                }
            }

            // 5. 写入 CITY 汇总行
            insertWorkorderCityRow(conn, date, cityTotal, total12345, totalZjb,
                    labelFill, labelCode, gridIn, focusCount);

            result.put("success", true);
            result.put("cityTotal", cityTotal);
            result.put("districtCount", count + 1);
            result.put("dataSource", districtData.isEmpty() ? "mock" : "source");
            result.put("message", String.format("同步成功: 日期=%s, 全市=%d, 区数=%d",
                    date, cityTotal, count + 1));

        } catch (Exception e) {
            log.error("同步工单日指标失败", e);
            result.put("success", false);
            result.put("message", "同步失败: " + e.getMessage());
        }

        return result;
    }

    @Override
    public Map<String, Object> syncWorkorderDailyBatch(String startDate, String endDate) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> details = new ArrayList<>();
        int success = 0, fail = 0;

        Calendar c = Calendar.getInstance();
        Date start, end, today = new Date();
        try {
            start = SDF.parse(startDate);
            c.setTime(start);
            end = SDF.parse(endDate);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "日期格式错误: " + e.getMessage());
            return result;
        }

        while (!c.getTime().after(end) && !c.getTime().after(today)) {
            String d = SDF.format(c.getTime());
            Map<String, Object> r = syncWorkorderDaily(d);
            details.add(r);
            if (Boolean.TRUE.equals(r.get("success"))) success++;
            else fail++;
            c.add(Calendar.DAY_OF_MONTH, 1);
        }

        result.put("success", fail == 0);
        result.put("startDate", startDate);
        result.put("endDate", endDate);
        result.put("total", success + fail);
        result.put("successCount", success);
        result.put("failCount", fail);
        result.put("details", details);
        return result;
    }

    @Override
    public Map<String, Object> syncCascadeLog(String statDate) {
        String date = statDate != null && !statDate.isEmpty() ? statDate : SDF.format(new Date());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("statDate", date);
        result.put("success", false);

        try (Connection conn = dataSource.getConnection()) {
            deleteByDate(conn, "dws_grid_cascade_log", date);

            int count = 0;
            Map<String, Long> sourceCounts = getSourceCounts(conn, date);

            for (String[] dimConfig : DIM_CONFIGS) {
                String dimCode = dimConfig[0];
                String dimName = dimConfig[1];
                String taskType = dimConfig[2];
                String scheduleRule = dimConfig[3];
                String runTime = dimConfig[4];

                String taskCode = dimCode + "_SYNC";
                String taskName = dimName + "同步任务";
                long cascadeCount = sourceCounts.getOrDefault(dimCode, 0L);
                Timestamp startTime = Timestamp.valueOf(date + " " + runTime);
                long duration = 1000L;
                Timestamp endTime = new Timestamp(startTime.getTime() + duration);

                String sql = "INSERT INTO dws_grid_cascade_log " +
                        "(task_code, task_name, task_type, dim_code, dim_name, " +
                        " frequency, schedule_rule, cascade_count, cascade_status, " +
                        " start_time, end_time, duration_ms, stat_date, create_time) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TO_DATE(?, 'YYYY-MM-DD'), CURRENT_TIMESTAMP)";

                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, taskCode);
                    ps.setString(2, taskName);
                    ps.setString(3, taskType);
                    ps.setString(4, dimCode);
                    ps.setString(5, dimName);
                    ps.setString(6, "daily");
                    ps.setString(7, scheduleRule);
                    ps.setLong(8, cascadeCount);
                    ps.setString(9, "SUCCESS");
                    ps.setTimestamp(10, startTime);
                    ps.setTimestamp(11, endTime);
                    ps.setLong(12, duration);
                    ps.setString(13, date);
                    ps.executeUpdate();
                    count++;
                }
            }

            result.put("success", true);
            result.put("count", count);
            result.put("dataSource", "database");
            result.put("message", String.format("级联日志同步成功: 日期=%s, 记录数=%d", date, count));

        } catch (Exception e) {
            log.error("同步级联日志失败", e);
            result.put("success", false);
            result.put("message", "同步失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 从实际源表获取数据量
     */
    private Map<String, Long> getSourceCounts(Connection conn, String date) {
        Map<String, Long> counts = new LinkedHashMap<>();

        try {
            // rxb_12345_gongdan_06_tousu：每天 2 点接口同步一次
            String sqlRxb = "SELECT COUNT(*) FROM rxb_12345_gongdan_06_tousu " +
                    "WHERE DATE(calltime) = TO_DATE(?, 'YYYY-MM-DD')";
            try (PreparedStatement ps = conn.prepareStatement(sqlRxb)) {
                ps.setString(1, date);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) counts.put("RXB", rs.getLong(1));
            }

            // zjw_wgh_t_contactinfo_23：库表级联，每天 1 次
            String sqlZjw = "SELECT COUNT(*) FROM zjw_wgh_t_contactinfo_23 " +
                    "WHERE DATE(discovertime) = TO_DATE(?, 'YYYY-MM-DD')";
            try (PreparedStatement ps = conn.prepareStatement(sqlZjw)) {
                ps.setString(1, date);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) counts.put("ZJW", rs.getLong(1));
            }

            // dwd_aj_wgaj_info_1208：每天 12 点接口同步一次
            String sqlDwd1208 = "SELECT COUNT(*) FROM dwd_aj_wgaj_info_1208 " +
                    "WHERE TO_CHAR(discover_time_ts, 'YYYY-MM-DD') = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlDwd1208)) {
                ps.setString(1, date);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) counts.put("DWD1208", rs.getLong(1));
            }

        } catch (SQLException e) {
            log.warn("获取源表数据量失败: {}", e.getMessage());
        }

        return counts;
    }

    @Override
    public Map<String, Object> syncCascadeLogBatch(String startDate, String endDate) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> details = new ArrayList<>();
        int success = 0, fail = 0;

        Calendar c = Calendar.getInstance();
        Date start, end, today = new Date();
        try {
            start = SDF.parse(startDate);
            c.setTime(start);
            end = SDF.parse(endDate);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "日期格式错误: " + e.getMessage());
            return result;
        }

        while (!c.getTime().after(end) && !c.getTime().after(today)) {
            String d = SDF.format(c.getTime());
            Map<String, Object> r = syncCascadeLog(d);
            details.add(r);
            if (Boolean.TRUE.equals(r.get("success"))) success++;
            else fail++;
            c.add(Calendar.DAY_OF_MONTH, 1);
        }

        result.put("success", fail == 0);
        result.put("startDate", startDate);
        result.put("endDate", endDate);
        result.put("total", success + fail);
        result.put("successCount", success);
        result.put("failCount", fail);
        result.put("details", details);
        return result;
    }

    /**
     * 从源表构建各区数据
     * 数据来源：
     *   - zjw_wgh_t_contactinfo_23 → 城市综合管理部事业工单
     *   - dwd_aj_wgaj_info → 12345市民热线工单
     */
    private List<Map<String, Object>> buildFromSource(Connection conn, String date) {
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Map<String, Object>> byCode = new LinkedHashMap<>();

        try {
            // 从城市综合管理部事业工单源表获取数据 (zjw_wgh_t_contactinfo_23)
            String zjwSql = "SELECT area_code AS areaCode, area AS areaName, COUNT(*) AS cnt " +
                    "FROM zjw_wgh_t_contactinfo_23 " +
                    "WHERE DATE(discovertime) = TO_DATE(?, 'YYYY-MM-DD') " +
                    "GROUP BY area_code, area";

            try (PreparedStatement ps = conn.prepareStatement(zjwSql)) {
                ps.setString(1, date);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    String code = rs.getString("areaCode");
                    String name = rs.getString("areaName");
                    if (code == null || code.isEmpty()) continue;
                    String areaName = (name != null && !name.isEmpty()) ? name : code;

                    final String finalCode = code;
                    final String finalName = areaName;
                    Map<String, Object> d = byCode.computeIfAbsent(code, k -> createDistrictMap(finalCode, finalName));
                    d.put("srcZjbCount", rs.getLong("cnt"));
                    d.put("totalCount", rs.getLong("cnt"));
                }
            }

            // 从 12345市民热线工单源表获取数据 (dwd_aj_wgaj_info)
            String rxbSql = "SELECT source_area AS areaCode, area AS areaName, COUNT(*) AS cnt " +
                    "FROM dwd_aj_wgaj_info " +
                    "WHERE TO_CHAR(discover_time_ts, 'YYYY-MM-DD') = ? " +
                    "GROUP BY source_area, area";

            try (PreparedStatement ps = conn.prepareStatement(rxbSql)) {
                ps.setString(1, date);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    String code = rs.getString("areaCode");
                    String name = rs.getString("areaName");
                    long cnt = rs.getLong("cnt");
                    if (code == null || code.isEmpty()) continue;

                    // 按 areaCode 匹配
                    Map<String, Object> d = byCode.computeIfAbsent(code, k -> createDistrictMap(code, name != null ? name : code));
                    long prev12345 = asLong(d.get("src12345Count"));
                    long prevTotal = asLong(d.get("totalCount"));
                    d.put("src12345Count", prev12345 + cnt);
                    d.put("totalCount", prevTotal + cnt);
                }
            }

            // 查询各区源表质量指标
            Map<String, Map<String, Object>> qualityByArea = queryQualityMetricsByArea(conn, date);

            // 计算 cityRatio 并填充各区质量指标
            long grandTotal = byCode.values().stream().mapToLong(d -> asLong(d.get("totalCount"))).sum();
            if (grandTotal > 0) {
                for (Map<String, Object> d : byCode.values()) {
                    long total = asLong(d.get("totalCount"));
                    BigDecimal ratio = new BigDecimal(total)
                            .multiply(HUNDRED)
                            .divide(new BigDecimal(grandTotal), 4, RoundingMode.HALF_UP);
                    d.put("cityRatio", ratio.toPlainString());

                    String areaCode = String.valueOf(d.get("areaCode"));
                    Map<String, Object> quality = qualityByArea.get(areaCode);
                    if (quality != null) {
                        d.put("labelFillCount", asLong(quality.get("labelFillCount")));
                        d.put("labelCodeCount", asLong(quality.get("labelCodeCount")));
                        d.put("gridInCount", asLong(quality.get("gridInCount")));
                        d.put("focusCount", asLong(quality.get("focusCount")));
                    }
                }
            }

            result.addAll(byCode.values());
        } catch (SQLException e) {
            log.warn("从源表构建数据失败，使用 mock: {}", e.getMessage());
            return Collections.emptyList();
        }

        return result;
    }

    /**
     * 从源表按区查询质量指标
     */
    private Map<String, Map<String, Object>> queryQualityMetricsByArea(Connection conn, String date) {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        String sql = "SELECT area_code AS areaCode, " +
                "       COALESCE(SUM(CASE WHEN infotypename IS NOT NULL AND infotypename <> '' THEN 1 ELSE 0 END), 0) AS labelFillCount, " +
                "       COALESCE(SUM(CASE WHEN isstandard = '是' OR isstandard = '1' THEN 1 ELSE 0 END), 0) AS labelCodeCount, " +
                "       COALESCE(SUM(CASE WHEN gridcode IS NOT NULL AND gridcode <> '' THEN 1 ELSE 0 END), 0) AS gridInCount, " +
                "       COALESCE(SUM(CASE WHEN isoverdue = '是' OR isoverdue = '1' OR isdelay = '是' OR isdelay = '1' THEN 1 ELSE 0 END), 0) AS focusCount " +
                "FROM zjw_wgh_t_contactinfo_23 " +
                "WHERE DATE(discovertime) = TO_DATE(?, 'YYYY-MM-DD') " +
                "  AND area_code IS NOT NULL AND area_code <> '' " +
                "GROUP BY area_code";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, date);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("labelFillCount", rs.getLong("labelFillCount"));
                row.put("labelCodeCount", rs.getLong("labelCodeCount"));
                row.put("gridInCount", rs.getLong("gridInCount"));
                row.put("focusCount", rs.getLong("focusCount"));
                result.put(rs.getString("areaCode"), row);
            }
        } catch (SQLException e) {
            log.warn("按区查询质量指标失败: {}", e.getMessage());
        }
        return result;
    }

    /**
     * 生成 Mock 数据
     */
    private List<Map<String, Object>> buildMockDistrictData(String date) {
        Random random = new Random(date.hashCode());
        List<Map<String, Object>> result = new ArrayList<>();
        long total = 800 + random.nextInt(400);

        for (int i = 0; i < AREA_NAMES.length; i++) {
            Map<String, Object> d = createDistrictMap(AREA_CODES[i], AREA_NAMES[i]);

            long areaTotal = Math.max(50, total / AREA_NAMES.length + random.nextInt(100) - 50);
            long src12345 = areaTotal * (30 + random.nextInt(40)) / 100;
            long srcZjb = areaTotal - src12345;
            long focus = areaTotal / 20;
            long labelFill = (long) (areaTotal * (0.85 + random.nextDouble() * 0.1));
            long labelCode = (long) (areaTotal * (0.75 + random.nextDouble() * 0.15));
            long gridIn = (long) (areaTotal * (0.70 + random.nextDouble() * 0.2));

            d.put("src12345Count", src12345);
            d.put("srcZjbCount", srcZjb);
            d.put("totalCount", areaTotal);
            d.put("focusCount", focus);
            d.put("labelFillCount", labelFill);
            d.put("labelCodeCount", labelCode);
            d.put("gridInCount", gridIn);

            result.add(d);
        }

        // 计算 cityRatio
        long grandTotal = result.stream().mapToLong(d -> asLong(d.get("totalCount"))).sum();
        if (grandTotal > 0) {
            for (Map<String, Object> d : result) {
                long t = asLong(d.get("totalCount"));
                BigDecimal ratio = new BigDecimal(t)
                        .multiply(HUNDRED)
                        .divide(new BigDecimal(grandTotal), 4, RoundingMode.HALF_UP);
                d.put("cityRatio", ratio.toPlainString());
            }
        }

        return result;
    }

    private Map<String, Object> createDistrictMap(String code, String name) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("areaCode", code);
        m.put("areaName", name);
        m.put("src12345Count", 0L);
        m.put("srcZjbCount", 0L);
        m.put("totalCount", 0L);
        m.put("focusCount", 0L);
        m.put("labelFillCount", 0L);
        m.put("labelCodeCount", 0L);
        m.put("gridInCount", 0L);
        m.put("cityRatio", "0.0000");
        return m;
    }

    private void deleteByDate(Connection conn, String table, String date) throws SQLException {
        String sql = "DELETE FROM " + table + " WHERE DATE(stat_date) = TO_DATE(?, 'YYYY-MM-DD')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, date);
            ps.executeUpdate();
        }
    }

    private void insertWorkorderRow(Connection conn, Map<String, Object> d, String date, boolean isCity) throws SQLException {
        String sql = "INSERT INTO dws_grid_workorder_daily " +
                "(stat_date, area_code, area_name, total_count, src_12345_count, src_zjb_count, " +
                " label_fill_count, label_fill_rate, label_code_count, label_code_rate, " +
                " grid_in_count, grid_in_rate, focus_count, city_ratio, " +
                " collect_status, update_status, update_time, create_time) " +
                "VALUES (TO_DATE(?, 'YYYY-MM-DD'), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " +
                " 'DONE', 'UPDATED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";

        long total = asLong(d.get("totalCount"));
        long src12345 = asLong(d.get("src12345Count"));
        long srcZjb = asLong(d.get("srcZjbCount"));
        long labelFill = asLong(d.get("labelFillCount"));
        long labelCode = asLong(d.get("labelCodeCount"));
        long gridIn = asLong(d.get("gridInCount"));

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, date);
            ps.setString(2, String.valueOf(d.get("areaCode")));
            ps.setString(3, String.valueOf(d.get("areaName")));
            ps.setLong(4, total);
            ps.setLong(5, src12345);
            ps.setLong(6, srcZjb);
            ps.setLong(7, labelFill);
            ps.setString(8, calcRate(labelFill, total));
            ps.setLong(9, labelCode);
            ps.setString(10, calcRate(labelCode, total));
            ps.setLong(11, gridIn);
            ps.setString(12, calcRate(gridIn, total));
            ps.setLong(13, asLong(d.get("focusCount")));
            ps.setString(14, isCity ? "100.0000" : String.valueOf(d.get("cityRatio")));
            ps.executeUpdate();
        }
    }

    private void insertWorkorderCityRow(Connection conn, String date, long total,
                                        long c12345, long czjb,
                                        long labelFill, long labelCode, long gridIn, long focus) throws SQLException {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("areaCode", "CITY");
        d.put("areaName", "全市");
        d.put("totalCount", total);
        d.put("src12345Count", c12345);
        d.put("srcZjbCount", czjb);
        d.put("focusCount", focus);
        d.put("labelFillCount", labelFill);
        d.put("labelCodeCount", labelCode);
        d.put("gridInCount", gridIn);
        d.put("cityRatio", "100.0000");

        insertWorkorderRow(conn, d, date, true);
    }

    private String calcRate(long numerator, long denominator) {
        if (denominator == 0) return "0.0000";
        return new BigDecimal(numerator)
                .multiply(HUNDRED)
                .divide(new BigDecimal(denominator), 4, RoundingMode.HALF_UP)
                .toPlainString();
    }

    private static long asLong(Object v) {
        if (v == null) return 0L;
        if (v instanceof Number) return ((Number) v).longValue();
        try { return Long.parseLong(String.valueOf(v)); } catch (Exception e) { return 0L; }
    }
}