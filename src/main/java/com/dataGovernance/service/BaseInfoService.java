package com.dataGovernance.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dataGovernance.domain.entity.GridConfig;
import com.dataGovernance.domain.entity.Rxb12345Gongdan06;

public interface BaseInfoService extends IService<Rxb12345Gongdan06> {

    void sourceToBaseGongdan();
    void dwdTo1208();

    void syncRLRecord(GridConfig gridConfig);

    /**
     * 同步投诉工单到SWB表
     * 
     * @param firstInsert true-首次插入（清空目标表后全量同步），false-增量同步（根据投诉表最大taskId判断增量数据）
     */
    void syncTousuToSWB(boolean firstInsert);

}
