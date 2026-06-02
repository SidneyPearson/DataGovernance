package com.dataGovernance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dataGovernance.domain.entity.DwsGridCascadeLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface DwsGridCascadeLogMapper extends BaseMapper<DwsGridCascadeLog> {

    /** 总览：库表/接口/文件分类总量（全部历史，与 sumAll 口径一致） */
    @Select("SELECT task_type AS taskType, " +
            "       COALESCE(SUM(cascade_count), 0) AS cnt " +
            "FROM dws_grid_cascade_log " +
            "GROUP BY task_type")
    List<Map<String, Object>> sumByType();

    /** 本月分类型数量 */
    @Select("SELECT task_type AS taskType, " +
            "       COALESCE(SUM(cascade_count), 0) AS cnt " +
            "FROM dws_grid_cascade_log " +
            "WHERE stat_date >= date_trunc('month', CURRENT_DATE) " +
            "GROUP BY task_type")
    List<Map<String, Object>> sumByTypeMonth();

    /** 上月分类型数量 */
    @Select("SELECT task_type AS taskType, " +
            "       COALESCE(SUM(cascade_count), 0) AS cnt " +
            "FROM dws_grid_cascade_log " +
            "WHERE stat_date >= date_trunc('month', CURRENT_DATE - INTERVAL '1 month') " +
            "  AND stat_date < date_trunc('month', CURRENT_DATE) " +
            "GROUP BY task_type")
    List<Map<String, Object>> sumByTypePrevMonth();

    /** 总览：按数据来源汇总（全部历史，与 sumAll 口径一致） */
    @Select("SELECT dim_code AS dimCode, dim_name AS dimName, " +
            "       COALESCE(SUM(cascade_count), 0) AS cnt " +
            "FROM dws_grid_cascade_log " +
            "GROUP BY dim_code, dim_name " +
            "ORDER BY cnt DESC")
    List<Map<String, Object>> sumByDim();

    /** 近4日总量（覆盖数据延迟场景） */
    @Select("SELECT COALESCE(SUM(cascade_count), 0) FROM dws_grid_cascade_log " +
            "WHERE stat_date >= CURRENT_DATE - INTERVAL '3 day'")
    Long sumToday();

    /** 历史总量 */
    @Select("SELECT COALESCE(SUM(cascade_count), 0) FROM dws_grid_cascade_log")
    Long sumAll();

    /** 再往前4日（用于计算变化量） */
    @Select("SELECT COALESCE(SUM(cascade_count), 0) FROM dws_grid_cascade_log " +
            "WHERE stat_date >= CURRENT_DATE - INTERVAL '7 day' AND stat_date < CURRENT_DATE - INTERVAL '3 day'")
    Long sumYesterday();

    /** 趋势图：近N日按日聚合（最后一天用近4日代替） */
    @Select("SELECT to_char(stat_date, 'YYYY-MM-DD') AS statDate, task_type AS taskType, " +
            "       COALESCE(SUM(cascade_count), 0) AS cnt " +
            "FROM dws_grid_cascade_log " +
            "WHERE stat_date >= CURRENT_DATE - (#{days}::int - 1) * INTERVAL '1 day' " +
            "GROUP BY to_char(stat_date, 'YYYY-MM-DD'), task_type " +
            "ORDER BY statDate")
    List<Map<String, Object>> trendByDay(@Param("days") int days);

    /** 趋势图最后一组：近4日各类型总量 */
    @Select("SELECT task_type AS taskType, COALESCE(SUM(cascade_count), 0) AS cnt " +
            "FROM dws_grid_cascade_log " +
            "WHERE stat_date >= CURRENT_DATE - INTERVAL '3 day' " +
            "GROUP BY task_type")
    List<Map<String, Object>> trendLast4DaysByType();

    /** 近 N 月按月聚合 */
    @Select("SELECT to_char(stat_date, 'YYYY-MM') AS month, " +
            "       COALESCE(SUM(cascade_count), 0) AS cnt " +
            "FROM dws_grid_cascade_log " +
            "WHERE stat_date >= (date_trunc('month', CURRENT_DATE) - (#{months}::int - 1) * INTERVAL '1 month') " +
            "GROUP BY to_char(stat_date, 'YYYY-MM') " +
            "ORDER BY month")
    List<Map<String, Object>> trendByMonth(@Param("months") int months);

    /** 详情列表：取近7日级联总量 + 历史累计 */
    @Select("<script>" +
            "SELECT t.task_code AS taskCode, t.task_name AS taskName, t.task_type AS taskType, " +
            "       t.dim_code AS dimCode, t.dim_name AS dimName, " +
            "       t.frequency AS frequency, t.schedule_rule AS scheduleRule, " +
            "       t.cascade_status AS cascadeStatus, t.start_time AS startTime, " +
            "       COALESCE(s.total, 0) AS totalCount, " +
            "       COALESCE(r.last7days, 0) AS last7DaysTotal " +
            "FROM dws_grid_cascade_log t " +
            "JOIN LATERAL (" +
            "  SELECT SUM(cascade_count) AS total FROM dws_grid_cascade_log WHERE task_code = t.task_code" +
            ") s ON true " +
            "JOIN LATERAL (" +
            "  SELECT SUM(cascade_count) AS last7days FROM dws_grid_cascade_log " +
            "  WHERE task_code = t.task_code AND stat_date >= CURRENT_DATE - INTERVAL '6 day'" +
            ") r ON true " +
            "JOIN ( " +
            "  SELECT task_code, MAX(start_time) AS mx FROM dws_grid_cascade_log " +
            "  WHERE stat_date >= CURRENT_DATE - INTERVAL '6 day' GROUP BY task_code" +
            ") m ON m.task_code = t.task_code AND m.mx = t.start_time " +
            "WHERE 1=1 " +
            "<if test='dimCode != null and dimCode != \"\"'>AND t.dim_code = #{dimCode}</if> " +
            "<if test='taskType != null and taskType != \"\"'>AND t.task_type = #{taskType}</if> " +
            "ORDER BY t.start_time DESC " +
            "LIMIT 200" +
            "</script>")
    List<Map<String, Object>> listLatestPerTask(@Param("dimCode") String dimCode,
                                                @Param("taskType") String taskType);

    /** 任务历史级联记录 */
    @Select("SELECT task_code AS taskCode, task_name AS taskName, " +
            "       cascade_count AS cascadeCount, cascade_status AS cascadeStatus, " +
            "       start_time AS startTime, end_time AS endTime, duration_ms AS durationMs " +
            "FROM dws_grid_cascade_log " +
            "WHERE task_code = #{taskCode} " +
            "ORDER BY start_time DESC " +
            "LIMIT 50")
    List<Map<String, Object>> listHistoryByTask(@Param("taskCode") String taskCode);
}