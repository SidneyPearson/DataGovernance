package com.dataGovernance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dataGovernance.domain.entity.TousuGongdanSWB;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TousuGongdanSWBMapper extends BaseMapper<TousuGongdanSWB> {

    @Select("<script>" +
            "SELECT wpid FROM rxb_12345_gongdan_06_tousu_swb " +
            "WHERE wpid IN " +
            "<foreach collection='wpidList' item='wpid' open='(' separator=',' close=')'>" +
            "#{wpid}" +
            "</foreach>" +
            "</script>")
    List<String> selectExistingWpids(@Param("wpidList") List<String> wpidList);

    @Select("SELECT COUNT(*) FROM rxb_12345_gongdan_06_tousu_swb")
    long count();

}