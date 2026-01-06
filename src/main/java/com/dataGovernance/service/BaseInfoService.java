package com.dataGovernance.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dataGovernance.domain.entity.GridConfig;
import com.dataGovernance.domain.entity.Rxb12345Gongdan06;

public interface BaseInfoService extends IService<Rxb12345Gongdan06> {

    void sourceToBaseGongdan();
    void dwdTo1208();

    void syncRLRecord(GridConfig gridConfig);

}
