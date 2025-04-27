package com.dataExtracting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dataExtracting.domain.entity.ZfDetail;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ZfDetailMapper extends BaseMapper<ZfDetail> {
    // 可以自定义一些方法
}
