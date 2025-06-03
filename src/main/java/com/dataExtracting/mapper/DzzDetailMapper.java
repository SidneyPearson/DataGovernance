package com.dataExtracting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dataExtracting.domain.entity.DzzDetail;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DzzDetailMapper extends BaseMapper<DzzDetail> {
    // 可以自定义一些方法
}
