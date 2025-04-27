package com.dataExtracting.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dataExtracting.domain.entity.District;
import com.dataExtracting.mapper.DistrictMapper;
import com.dataExtracting.service.DistrictService;
import org.springframework.stereotype.Service;


@Service
public class DistrictServiceImpl extends ServiceImpl<DistrictMapper, District>
        implements DistrictService {


}
