package com.dataExtracting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dataExtracting.domain.entity.BaseInfoV2;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BaseInfoV2Mapper extends BaseMapper<BaseInfoV2> {
    // 可以自定义一些方法
}
