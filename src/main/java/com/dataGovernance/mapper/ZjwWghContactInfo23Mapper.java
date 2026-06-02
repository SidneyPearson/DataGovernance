package com.dataGovernance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dataGovernance.domain.entity.ZjwWghContactInfo23;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ZjwWghContactInfo23Mapper extends BaseMapper<ZjwWghContactInfo23> {

    /** 工单清单：按区/状态/页大小列出 */
    @Select("<script>" +
            "SELECT taskid AS taskId, infosourcename AS infoSourceName, " +
            "       infotypename AS infoTypeName, infoscname AS infoScName, " +
            "       areacode AS areaCode, areaname AS areaName, " +
            "       streetname AS streetName, gridcode AS gridCode, address, " +
            "       discovertime AS discoverTime, accepttime AS acceptTime, " +
            "       endtime AS endTime, nowstatusname AS nowStatusName, " +
            "       isoverdue AS isOverdue, isstandard AS isStandard, isend AS isEnd " +
            "FROM zjw_wgh_t_contactinfo_23 WHERE 1=1 " +
            "<if test='areaCode != null and areaCode != \"\" and areaCode != \"all\"'>AND areacode = #{areaCode}</if> " +
            "<if test='startDate != null and startDate != \"\"'>AND discovertime &gt;= TO_DATE(#{startDate}, 'YYYY-MM-DD')</if> " +
            "<if test='endDate != null and endDate != \"\"'>AND discovertime &lt; TO_DATE(#{endDate}, 'YYYY-MM-DD') + INTERVAL '1 day'</if> " +
            "ORDER BY discovertime DESC NULLS LAST " +
            "LIMIT #{pageSize}" +
            "</script>")
    List<Map<String, Object>> selectList(@Param("areaCode") String areaCode,
                                         @Param("startDate") String startDate,
                                         @Param("endDate") String endDate,
                                         @Param("pageSize") int pageSize);

    /** 重点关注（督办/超期/延期） */
    @Select("<script>" +
            "SELECT taskid AS taskId, areaname AS areaName, infotypename AS focusReason, " +
            "       discovertime AS discoverTime, nowstatusname AS nowStatusName, " +
            "       CASE WHEN isoverdue = '是' OR isoverdue = '1' THEN '高' " +
            "            WHEN isdelay = '是' OR isdelay = '1' THEN '中' " +
            "            ELSE '低' END AS focusLevel " +
            "FROM zjw_wgh_t_contactinfo_23 " +
            "WHERE (isoverdue = '是' OR isoverdue = '1' OR isdelay = '是' OR isdelay = '1') " +
            "<if test='statDate != null and statDate != \"\"'>" +
            "  AND DATE(discovertime) = TO_DATE(#{statDate}, 'YYYY-MM-DD')" +
            "</if> " +
            "ORDER BY discovertime DESC NULLS LAST LIMIT 50" +
            "</script>")
    List<Map<String, Object>> selectFocus(@Param("statDate") String statDate);

    /** 各区当日工单计数（fallback 当 dws 表没数据时按源表实时聚合） */
    @Select("<script>" +
            "SELECT areacode AS areaCode, areaname AS areaName, COUNT(*) AS cnt " +
            "FROM zjw_wgh_t_contactinfo_23 " +
            "WHERE 1=1 " +
            "<choose>" +
            "  <when test='statDate != null and statDate != \"\"'>" +
            "    AND DATE(discovertime) = TO_DATE(#{statDate}, 'YYYY-MM-DD')" +
            "  </when>" +
            "  <otherwise>" +
            "    AND DATE(discovertime) = CURRENT_DATE" +
            "  </otherwise>" +
            "</choose> " +
            "GROUP BY areacode, areaname " +
            "ORDER BY cnt DESC" +
            "</script>")
    List<Map<String, Object>> countByArea(@Param("statDate") String statDate);

    @Select("SELECT COUNT(*) FROM zjw_wgh_t_contactinfo_23 WHERE DATE(discovertime) = CURRENT_DATE")
    Long countToday();

    @Select("SELECT COUNT(*) FROM zjw_wgh_t_contactinfo_23 WHERE DATE(discovertime) = TO_DATE(#{statDate}, 'YYYY-MM-DD')")
    Long countByDate(@Param("statDate") String statDate);

    @Select("SELECT COUNT(*) FROM zjw_wgh_t_contactinfo_23 WHERE DATE(discovertime) = CURRENT_DATE - INTERVAL '1 day'")
    Long countYesterday();

    @Select("SELECT COUNT(*) FROM zjw_wgh_t_contactinfo_23")
    Long countAll();

    /** 标签填报率：infotypename / infoscname 是否非空，历史全量 */
    @Select("SELECT COALESCE(SUM(CASE WHEN infotypename IS NOT NULL AND infotypename <> '' THEN 1 ELSE 0 END), 0) AS filled, " +
            "       COUNT(*) AS total " +
            "FROM zjw_wgh_t_contactinfo_23")
    Map<String, Object> labelFillAll();

    /** 工单入格率：gridcode 非空，历史全量 */
    @Select("SELECT COALESCE(SUM(CASE WHEN gridcode IS NOT NULL AND gridcode <> '' THEN 1 ELSE 0 END), 0) AS gridIn, " +
            "       COUNT(*) AS total " +
            "FROM zjw_wgh_t_contactinfo_23")
    Map<String, Object> gridInAll();

    /** 标签编码率：isstandard 为「是/1」，历史全量 */
    @Select("SELECT COALESCE(SUM(CASE WHEN isstandard = '是' OR isstandard = '1' THEN 1 ELSE 0 END), 0) AS coded, " +
            "       COUNT(*) AS total " +
            "FROM zjw_wgh_t_contactinfo_23")
    Map<String, Object> labelCodeAll();

    /** 重点关注计数，历史全量 */
    @Select("SELECT COUNT(*) FROM zjw_wgh_t_contactinfo_23 " +
            "WHERE (isoverdue = '是' OR isoverdue = '1' OR isdelay = '是' OR isdelay = '1')")
    Long focusCountAll();
}
