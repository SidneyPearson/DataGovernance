package com.dataExtracting.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dataExtracting.domain.entity.BaseInfoV2;
import com.dataExtracting.domain.entity.District;

public interface BaseInfoV2Service extends IService<BaseInfoV2> {

    void sourceToBase(District district, boolean isFirst);

    void baseToDmTarget();

    void backupV2();

}
