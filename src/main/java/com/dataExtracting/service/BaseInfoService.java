package com.dataExtracting.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dataExtracting.domain.entity.BaseInfo;
import com.dataExtracting.domain.entity.District;

import java.util.List;

public interface BaseInfoService extends IService<BaseInfo> {

    void sourceToBase(District district, boolean isFirst);

    void baseToTarget();

}
