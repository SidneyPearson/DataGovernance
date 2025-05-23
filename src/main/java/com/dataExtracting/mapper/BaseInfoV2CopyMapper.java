package com.dataExtracting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dataExtracting.domain.entity.BaseInfoV2Copy;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BaseInfoV2CopyMapper extends BaseMapper<BaseInfoV2Copy> {
    // 可以自定义一些方法
}
