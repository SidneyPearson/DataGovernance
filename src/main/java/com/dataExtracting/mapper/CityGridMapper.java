package com.dataExtracting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dataExtracting.domain.entity.CityGrid;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CityGridMapper extends BaseMapper<CityGrid> {
    // 可以自定义一些方法
}
