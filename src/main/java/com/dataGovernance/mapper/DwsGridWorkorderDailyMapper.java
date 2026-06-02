package com.dataGovernance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dataGovernance.domain.entity.DwsGridWorkorderDaily;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface DwsGridWorkorderDailyMapper extends BaseMapper<DwsGridWorkorderDaily> {

    /** 区域下拉：从 dws 表按 area_code/area_name 去重 */
    @Select("SELECT area_code AS areaCode, MAX(area_name) AS areaName " +
            "FROM dws_grid_workorder_daily " +
            "WHERE area_code IS NOT NULL AND area_code <> '' " +
            "GROUP BY area_code " +
            "ORDER BY CASE WHEN lower(area_code) = 'city' THEN 0 ELSE 1 END, MAX(area_name)")
    List<Map<String, Object>> selectAreaOptions();

    /** 全市前2日聚合（CITY 行为口径）- 使用范围查询 */
    @Select("SELECT * FROM dws_grid_workorder_daily " +
            "WHERE area_code = 'CITY' " +
            "AND stat_date >= CURRENT_DATE - INTERVAL '2 day' " +
            "AND stat_date < CURRENT_DATE - INTERVAL '1 day' " +
            "LIMIT 1")
    DwsGridWorkorderDaily selectCity2DaysAgo();

    /** 全市前3日聚合（用于对比） */
    @Select("SELECT * FROM dws_grid_workorder_daily " +
            "WHERE area_code = 'CITY' " +
            "AND stat_date >= CURRENT_DATE - INTERVAL '3 day' " +
            "AND stat_date < CURRENT_DATE - INTERVAL '2 day' " +
            "LIMIT 1")
    DwsGridWorkorderDaily selectCity3DaysAgo();

    /** 历史汇集总量 */
    @Select("SELECT COALESCE(SUM(total_count), 0) FROM dws_grid_workorder_daily WHERE area_code = 'CITY'")
    Long sumCityAll();

    /** 指定区域历史汇集总量 */
    @Select("SELECT COALESCE(SUM(total_count), 0) FROM dws_grid_workorder_daily WHERE area_code = #{areaCode}")
    Long sumAreaAll(@Param("areaCode") String areaCode);

    /** 工单类型分布：指定区域历史累计按类型求和 */
    @Select("<script>" +
            "SELECT '12345' AS src_type, COALESCE(SUM(src_12345_count), 0) AS cnt " +
            "FROM dws_grid_workorder_daily WHERE 1=1 " +
            "<choose>" +
            "  <when test='areaCode != null and areaCode != \"\" and areaCode != \"all\"'>AND area_code = #{areaCode}</when>" +
            "  <otherwise>AND area_code = 'CITY'</otherwise>" +
            "</choose> " +
            "UNION ALL " +
            "SELECT 'ZJB' AS src_type, COALESCE(SUM(src_zjb_count), 0) AS cnt " +
            "FROM dws_grid_workorder_daily WHERE 1=1 " +
            "<choose>" +
            "  <when test='areaCode != null and areaCode != \"\" and areaCode != \"all\"'>AND area_code = #{areaCode}</when>" +
            "  <otherwise>AND area_code = 'CITY'</otherwise>" +
            "</choose>" +
            "</script>")
    List<Map<String, Object>> selectTypeDistribution(@Param("areaCode") String areaCode);

    /** 各区历史累计汇总：所有区 total_count 相加应等于 CITY 历史汇集总量 */
    @Select("SELECT area_code AS areaCode, MAX(area_name) AS areaName, " +
            "       COALESCE(SUM(total_count), 0) AS totalCount, " +
            "       COALESCE(SUM(src_12345_count), 0) AS src12345Count, " +
            "       COALESCE(SUM(src_zjb_count), 0) AS srcZjbCount, " +
            "       COALESCE(SUM(label_fill_count), 0) AS labelFillCount, " +
            "       CASE WHEN COALESCE(SUM(total_count), 0) = 0 THEN 0 " +
            "            ELSE ROUND(COALESCE(SUM(label_fill_count), 0) * 100.0 / SUM(total_count), 4) END AS labelFillRate, " +
            "       COALESCE(SUM(label_code_count), 0) AS labelCodeCount, " +
            "       CASE WHEN COALESCE(SUM(total_count), 0) = 0 THEN 0 " +
            "            ELSE ROUND(COALESCE(SUM(label_code_count), 0) * 100.0 / SUM(total_count), 4) END AS labelCodeRate, " +
            "       COALESCE(SUM(grid_in_count), 0) AS gridInCount, " +
            "       CASE WHEN COALESCE(SUM(total_count), 0) = 0 THEN 0 " +
            "            ELSE ROUND(COALESCE(SUM(grid_in_count), 0) * 100.0 / SUM(total_count), 4) END AS gridInRate, " +
            "       COALESCE(SUM(focus_count), 0) AS focusCount, " +
            "       CASE WHEN SUM(SUM(total_count)) OVER () = 0 THEN 0 " +
            "            ELSE ROUND(SUM(total_count) * 100.0 / SUM(SUM(total_count)) OVER (), 4) END AS cityRatio, " +
            "       'DONE' AS collectStatus, 'UPDATED' AS updateStatus, " +
            "       MAX(stat_date) AS collectTime " +
            "FROM dws_grid_workorder_daily " +
            "WHERE area_code != 'CITY' " +
            "GROUP BY area_code " +
            "ORDER BY totalCount DESC NULLS LAST")
    List<DwsGridWorkorderDaily> selectDistrictsAll();

    /** 各区指定日期一行 */
    @Select("<script>" +
            "SELECT * FROM dws_grid_workorder_daily " +
            "WHERE area_code != 'CITY' " +
            "<choose>" +
            "  <when test='statDate != null and statDate != \"\"'>" +
            "    AND stat_date &gt;= TO_DATE(#{statDate}, 'YYYY-MM-DD')" +
            "    AND stat_date &lt; TO_DATE(#{statDate}, 'YYYY-MM-DD') + INTERVAL '1 day'" +
            "  </when>" +
            "  <otherwise>" +
            "    AND stat_date &gt;= CURRENT_DATE" +
            "    AND stat_date &lt; CURRENT_DATE + INTERVAL '1 day'" +
            "  </otherwise>" +
            "</choose> " +
            "ORDER BY total_count DESC NULLS LAST" +
            "</script>")
    List<DwsGridWorkorderDaily> selectDistricts(@Param("statDate") String statDate);

    /** 单个区指定日期 */
    @Select("<script>" +
            "SELECT * FROM dws_grid_workorder_daily " +
            "WHERE area_code = #{areaCode} " +
            "<choose>" +
            "  <when test='statDate != null and statDate != \"\"'>" +
            "    AND stat_date &gt;= TO_DATE(#{statDate}, 'YYYY-MM-DD')" +
            "    AND stat_date &lt; TO_DATE(#{statDate}, 'YYYY-MM-DD') + INTERVAL '1 day'" +
            "  </when>" +
            "  <otherwise>" +
            "    AND stat_date &gt;= CURRENT_DATE" +
            "    AND stat_date &lt; CURRENT_DATE + INTERVAL '1 day'" +
            "  </otherwise>" +
            "</choose> " +
            "LIMIT 1" +
            "</script>")
    DwsGridWorkorderDaily selectByAreaAndDate(@Param("areaCode") String areaCode,
                                              @Param("statDate") String statDate);

    /** 趋势：按日期 + 区聚合 */
    @Select("<script>" +
            "SELECT to_char(stat_date, 'YYYY-MM-DD') AS statDate, " +
            "       COALESCE(total_count, 0) AS totalCount, " +
            "       COALESCE(new_count, 0) AS newCount " +
            "FROM dws_grid_workorder_daily " +
            "WHERE area_code = " +
            "  <choose>" +
            "    <when test='areaCode != null and areaCode != \"\" and areaCode != \"all\"'>#{areaCode}</when>" +
            "    <otherwise>'CITY'</otherwise>" +
            "  </choose> " +
            "AND stat_date &gt;= CURRENT_DATE - (#{days}::int - 1) * INTERVAL '1 day' " +
            "ORDER BY stat_date" +
            "</script>")
    List<Map<String, Object>> selectTrend(@Param("areaCode") String areaCode,
                                          @Param("days") int days);

    /** 周聚合 */
    @Select("<script>" +
            "SELECT to_char(date_trunc('week', stat_date), 'YYYY-MM-DD') AS weekStart, " +
            "       COALESCE(SUM(total_count), 0) AS totalCount, " +
            "       COALESCE(SUM(new_count), 0) AS newCount " +
            "FROM dws_grid_workorder_daily " +
            "WHERE area_code = " +
            "  <choose>" +
            "    <when test='areaCode != null and areaCode != \"\" and areaCode != \"all\"'>#{areaCode}</when>" +
            "    <otherwise>'CITY'</otherwise>" +
            "  </choose> " +
            "AND stat_date &gt;= CURRENT_DATE - (#{weeks}::int * 7 - 1) * INTERVAL '1 day' " +
            "GROUP BY date_trunc('week', stat_date) " +
            "ORDER BY weekStart" +
            "</script>")
    List<Map<String, Object>> selectTrendByWeek(@Param("areaCode") String areaCode,
                                                @Param("weeks") int weeks);

    /** 归集日历（按月） */
    @Select("<script>" +
            "SELECT to_char(stat_date, 'YYYY-MM-DD') AS statDate, " +
            "       collect_status AS collectStatus, " +
            "       to_char(collect_time, 'YYYY-MM-DD HH24:MI') AS collectTime, " +
            "       COALESCE(total_count, 0) AS collectCount " +
            "FROM dws_grid_workorder_daily " +
            "WHERE area_code = 'CITY' " +
            "AND to_char(stat_date, 'YYYY-MM') = #{month} " +
            "ORDER BY stat_date" +
            "</script>")
    List<Map<String, Object>> selectCalendar(@Param("month") String month);
}