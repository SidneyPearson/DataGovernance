package com.dataExtracting.job;

import com.dataExtracting.domain.entity.District;
import com.dataExtracting.service.BaseInfoService;
import com.dataExtracting.service.BaseInfoV2Service;
import com.dataExtracting.service.DistrictService;
import com.dataExtracting.service.ZfDetailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;


@Component
public class ScheduledTasks {

    @Autowired
    private DistrictService districtService;
    @Autowired
    private BaseInfoService baseInfoService;
    @Autowired
    private BaseInfoV2Service baseInfoV2Service;
    @Autowired
    private ZfDetailService zfDetailService;

    private static final Logger log = LoggerFactory.getLogger(ScheduledTasks.class);
    
    // 使用cron表达式，每天上午07:50执行
    @Scheduled(cron = "0 50 07 * * ?")
    public void sourceToBaseUpdateTask() {
        log.info("执行【抽取更新处理全市各区新兴领域党建数据基础信息表数据】定时任务，当前时间: {}", LocalDateTime.now());
        List<District> districtList = districtService.list();
        for (District district : districtList) {
            log.info("开始处理【{}】信息表数据", district.getName());
            baseInfoService.sourceToBase(district, false);
        }
        log.info("【抽取更新处理全市各区新兴领域党建数据基础信息表数据】定时任务执行完毕，当前时间: {}", LocalDateTime.now());

        log.info("执行【推送新兴领域党建数据基础信息表数据到目标表】定时任务，当前时间: {}", LocalDateTime.now());
        baseInfoService.baseToDmTarget();
        log.info("【推送新兴领域党建数据基础信息表数据到目标表】定时任务执行完毕，当前时间: {}", LocalDateTime.now());
    }

    // 使用cron表达式，每天上午07:40执行
    @Scheduled(cron = "0 40 07 * * ?")
    public void sourceToBaseV2UpdateTask() {
        log.info("执行【抽取更新处理全市各区新兴领域党建数据基础信息表V2数据】定时任务，当前时间: {}", LocalDateTime.now());
        List<District> districtList = districtService.list();
        for (District district : districtList) {
            log.info("开始处理【{}】信息表数据", district.getName());
            baseInfoV2Service.sourceToBase(district, false);
        }
        log.info("【抽取更新处理全市各区新兴领域党建数据基础信息表V2数据】定时任务执行完毕，当前时间: {}", LocalDateTime.now());

        log.info("执行【推送新兴领域党建数据基础信息表V2数据到目标表】定时任务，当前时间: {}", LocalDateTime.now());
        baseInfoV2Service.baseToDmTarget();
        log.info("【推送新兴领域党建数据基础信息表V2数据到目标表】定时任务执行完毕，当前时间: {}", LocalDateTime.now());
    }

    @Scheduled(cron = "0 0 * * * ?")
    public void sourceToBaseDetailTask() {
        log.info("执行【抽取处理全市各区新兴领域党建数据走访数据表数据】定时任务，当前时间: {}", LocalDateTime.now());
        List<District> districtList = districtService.list();
        for (District district : districtList) {
            log.info("开始处理【{}】信息表数据", district.getName());
            zfDetailService.sourceToBase(district, false);
        }
        log.info("【抽取处理全市各区新兴领域党建数据走访数据表数据】定时任务执行完毕，当前时间: {}", LocalDateTime.now());

        log.info("执行【推送新兴领域党建数据走访数据数据到目标表】定时任务，当前时间: {}", LocalDateTime.now());
        zfDetailService.baseToDmTarget();
        log.info("【推送新新兴领域党建数据走访数据到目标表】定时任务执行完毕，当前时间: {}", LocalDateTime.now());
    }

    @Scheduled(cron = "0 30 08 * * ?")
    public void backupV2() {
        log.info("开始备份基础V2表数据，当前时间: {}", LocalDateTime.now());
        baseInfoV2Service.backupV2();
        log.info("结束备份基础V2表数据，当前时间: {}", LocalDateTime.now());
    }
    
}