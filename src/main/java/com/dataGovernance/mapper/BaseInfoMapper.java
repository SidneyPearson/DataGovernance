package com.dataGovernance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dataGovernance.domain.entity.Rxb12345Gongdan06;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface BaseInfoMapper extends BaseMapper<Rxb12345Gongdan06> {

    @Delete("TRUNCATE TABLE rxb_12345_gongdan_06_tousu")
    void truncateTable();

    @Insert("<script>" +
            "INSERT INTO rxb_12345_gongdan_06_tousu " +
            "(wpid, calltime, callnum, rel_name, gender, rel_district, rel_address, " +
            "call_type, isrepeat, wp_source, wp_type, class1, class2, class3, class4, " +
            "summary, supervision, dept_level2, wp_customertype, wp_servicetype, " +
            "rel_phoneno, hurry_count, priority, note, callid, new_class1, new_class2, " +
            "new_class3, new_class4, new_class5, dept_level3, create_time, dsjzx_taskid) " +
            "VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.wpid}, #{item.calltime}, #{item.callnum}, #{item.relName}, #{item.gender}, " +
            "#{item.relDistrict}, #{item.relAddress}, #{item.callType}, #{item.isrepeat}, " +
            "#{item.wpSource}, #{item.wpType}, #{item.class1}, #{item.class2}, #{item.class3}, " +
            "#{item.class4}, #{item.summary}, #{item.supervision}, #{item.deptLevel2}, " +
            "#{item.wpCustomertype}, #{item.wpServicetype}, #{item.relPhoneno}, " +
            "#{item.hurryCount}, #{item.priority}, #{item.note}, #{item.callid}, " +
            "#{item.newClass1}, #{item.newClass2}, #{item.newClass3}, #{item.newClass4}, " +
            "#{item.newClass5}, #{item.deptLevel3}, #{item.createTime}, #{item.dsjzxTaskid})" +
            "</foreach>" +
            "</script>")
    void insertBatch(@Param("list") List<Rxb12345Gongdan06> list);

    /** 工单清单（12345 来源） */
    @Select("<script>" +
            "SELECT wpid AS taskId, wp_type AS wpType, wp_source AS wpSource, " +
            "       rel_district AS areaName, calltime AS createTime, " +
            "       summary AS summary, supervision AS supervision, " +
            "       class1, class2, class3, dept_level2 AS deptLevel2 " +
            "FROM rxb_12345_gongdan_06_tousu " +
            "WHERE 1=1 " +
            "<if test='district != null and district != \"\"'>AND rel_district = #{district}</if> " +
            "<if test='startDate != null and startDate != \"\"'>AND calltime &gt;= TO_DATE(#{startDate}, 'YYYY-MM-DD')</if> " +
            "<if test='endDate != null and endDate != \"\"'>AND calltime &lt; TO_DATE(#{endDate}, 'YYYY-MM-DD') + INTERVAL '1 day'</if> " +
            "ORDER BY calltime DESC NULLS LAST LIMIT #{pageSize}" +
            "</script>")
    List<Map<String, Object>> selectWorkorderList(@Param("district") String district,
                                                  @Param("startDate") String startDate,
                                                  @Param("endDate") String endDate,
                                                  @Param("pageSize") int pageSize);

    /** 重点关注（督办 / 多次催办） */
    @Select("<script>" +
            "SELECT wpid AS taskId, rel_district AS areaName, " +
            "       CASE WHEN supervision = '是' OR supervision = '1' THEN '督办' " +
            "            WHEN COALESCE(hurry_count, '0')::int >= 3 THEN '多次催办' " +
            "            WHEN isrepeat = '是' OR isrepeat = '1' THEN '重复来电' " +
            "            ELSE '高优先级' END AS focusReason, " +
            "       calltime AS discoverTime, " +
            "       CASE WHEN priority = '高' OR priority = '1' THEN '高' ELSE '中' END AS focusLevel " +
            "FROM rxb_12345_gongdan_06_tousu " +
            "WHERE (supervision = '是' OR supervision = '1' " +
            "       OR COALESCE(hurry_count, '0')::int >= 3 " +
            "       OR isrepeat = '是' OR isrepeat = '1') " +
            "<if test='statDate != null and statDate != \"\"'>" +
            "  AND DATE(calltime) = TO_DATE(#{statDate}, 'YYYY-MM-DD')" +
            "</if> " +
            "ORDER BY calltime DESC NULLS LAST LIMIT 50" +
            "</script>")
    List<Map<String, Object>> selectFocusList(@Param("statDate") String statDate);

    @Select("SELECT COUNT(*) FROM rxb_12345_gongdan_06_tousu WHERE DATE(calltime) = CURRENT_DATE")
    Long countToday();

    @Select("SELECT COUNT(*) FROM rxb_12345_gongdan_06_tousu WHERE DATE(calltime) = TO_DATE(#{statDate}, 'YYYY-MM-DD')")
    Long countByDate(@Param("statDate") String statDate);

    @Select("SELECT COUNT(*) FROM rxb_12345_gongdan_06_tousu WHERE DATE(calltime) = CURRENT_DATE - INTERVAL '1 day'")
    Long countYesterday();

    @Select("SELECT COUNT(*) FROM rxb_12345_gongdan_06_tousu")
    Long countAll();

    @Select("<script>" +
            "SELECT rel_district AS areaName, COUNT(*) AS cnt " +
            "FROM rxb_12345_gongdan_06_tousu " +
            "<choose>" +
            "  <when test='statDate != null and statDate != \"\"'>" +
            "    WHERE DATE(calltime) = TO_DATE(#{statDate}, 'YYYY-MM-DD')" +
            "  </when>" +
            "  <otherwise>WHERE DATE(calltime) = CURRENT_DATE</otherwise>" +
            "</choose> " +
            "GROUP BY rel_district ORDER BY cnt DESC" +
            "</script>")
    List<Map<String, Object>> countByDistrict(@Param("statDate") String statDate);
}
