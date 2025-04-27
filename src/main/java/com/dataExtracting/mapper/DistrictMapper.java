package com.dataExtracting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dataExtracting.domain.entity.District;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DistrictMapper extends BaseMapper<District> {
    // 可以自定义一些方法
}
