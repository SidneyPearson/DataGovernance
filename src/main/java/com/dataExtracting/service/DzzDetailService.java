package com.dataExtracting.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dataExtracting.domain.entity.DzzDetail;

public interface DzzDetailService extends IService<DzzDetail> {

    void getDzzData();
    void pushDzzData();

}
