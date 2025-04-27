package com.dataExtracting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dataExtracting.domain.entity.BaseInfo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BaseInfoMapper extends BaseMapper<BaseInfo> {
    // 可以自定义一些方法
}
