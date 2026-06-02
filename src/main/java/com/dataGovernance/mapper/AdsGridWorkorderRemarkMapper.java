package com.dataGovernance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dataGovernance.domain.entity.AdsGridWorkorderRemark;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface AdsGridWorkorderRemarkMapper extends BaseMapper<AdsGridWorkorderRemark> {

    @Select("SELECT task_id AS taskId, COUNT(*) AS cnt " +
            "FROM ads_grid_workorder_remark " +
            "WHERE del_flag = 0 AND task_id IN " +
            "<script><foreach collection='taskIds' item='it' open='(' separator=',' close=')'>#{it}</foreach></script> " +
            "GROUP BY task_id")
    List<Map<String, Object>> countByTasks(@Param("taskIds") List<String> taskIds);
}
