package com.dataGovernance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dataGovernance.domain.entity.Rxb12345Gongdan06;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BaseInfoMapper extends BaseMapper<Rxb12345Gongdan06> {
    // 可以自定义一些方法

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
}
