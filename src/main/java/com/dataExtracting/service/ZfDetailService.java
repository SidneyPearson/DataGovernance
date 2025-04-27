package com.dataExtracting.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dataExtracting.domain.entity.District;
import com.dataExtracting.domain.entity.ZfDetail;

public interface ZfDetailService extends IService<ZfDetail> {

    void sourceToBase(District district, boolean isFirst);

    void baseToTarget();

}
