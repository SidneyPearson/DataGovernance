package com.dataExtracting.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dataExtracting.domain.entity.CityGrid;
import com.dataExtracting.mapper.CityGridMapper;
import com.dataExtracting.service.CityGridService;
import org.springframework.stereotype.Service;


@Service
public class CityGridServiceImpl extends ServiceImpl<CityGridMapper, CityGrid>
        implements CityGridService {


}
