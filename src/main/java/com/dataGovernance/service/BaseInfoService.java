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

    /**
     * 从数据治理部同步SGB_XSKB_SHENHE表到目标库
     * 
     * 根据目标表是否有数据自动判断首次/增量同步
     */
    void syncSgbShenheFromDataProcess();

    /**
     * 从数据治理部同步SGB_XSKB_SHENQING表到目标库
     * 
     * 根据目标表是否有数据自动判断首次/增量同步
     */
    void syncSgbShenqingFromDataProcess();

    /**
     * 维护历史数据
     * 
     * @param tableName 表名（SHENQING, SHENHE, ZOUFANG）
     * @param reParseTagName 是否重新解析 tagName（仅对申请表有效）
     * @param clearData 是否先清空目标表数据
     * @param taskIdFilter 按 DSJZX_TASKID 过滤（为空则不限制）
     */
    void maintainHistoryData(String tableName, boolean reParseTagName, boolean clearData, String taskIdFilter);

    /**
     * 从数据治理部同步SGB_XSKB_ZOUFANG表到目标库
     * 
     * 根据目标表是否有数据自动判断首次/增量同步
     */
    void syncSgbZoufangFromDataProcess();

}
