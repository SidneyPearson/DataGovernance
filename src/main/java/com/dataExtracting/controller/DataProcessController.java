package com.dataExtracting.controller;

import com.dataExtracting.domain.entity.*;
import com.dataExtracting.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/data")
public class DataProcessController extends BaseController{

    @Autowired
    private BaseInfoService baseInfoService;
    @Autowired
    private BaseInfoV2Service baseInfoV2Service;
    @Autowired
    private DistrictService districtService;
    @Autowired
    private ZfDetailService zfDetailService;
    @Autowired
    private DzzDetailService dzzDetailService;

    private static final Logger log = LoggerFactory.getLogger(DataProcessController.class);

    /**
     * 抽取各区新兴领域党建数据基础信息表，经过处理保存到市基础信息表
     * @param code 默认不传则处理全市各区，传区code处理指定区，传"0000"为首次处理,start有值则视为首次
     */
    @GetMapping("/sourceToBase")
    public void sourceToBase(@RequestParam(value = "code", required = false) String code,
                             @RequestParam(value = "start", required = false) String start) {
        List<District> districtList = districtService.list();
        districtList = districtList.stream().filter(d -> "Y".equals(d.getXxlyInfoSyn()))
                .collect(Collectors.toList());
        boolean isFirstTime = "0000".equals(code);

        log.info("手动触发【{}抽取处理新兴领域党建数据基础信息表数据】任务，当前时间: {}",
                isFirstTime ? "首次" : "", LocalDateTime.now());

        if (code != null && !isFirstTime) {
            District district = districtList.stream()
                    .filter(d -> code.equals(d.getCode()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("根据code没有匹配到对应的区！"));
            districtList = Collections.singletonList(district);
        }

        if (start != null) {
            isFirstTime = true;
        }

        for (District district : districtList) {
            log.info("开始处理【{}】信息表数据", district.getName());
            baseInfoService.sourceToBase(district, isFirstTime);
        }

//        log.info("开始向目标表推送新兴领域党建数据基础信息");
//        baseInfoService.baseToTarget();
        log.info("任务执行完毕，当前时间: {}", LocalDateTime.now());
    }

    /**
     * 抽取各区新兴领域党建数据基础信息表，经过处理保存到市基础信息表
     * @param code 默认不传则处理全市各区，传区code处理指定区，传"0000"为首次处理,start有值则视为首次
     */
    @GetMapping("/sourceToBaseV2")
    public void sourceToBaseV2(@RequestParam(value = "code", required = false) String code,
                             @RequestParam(value = "start", required = false) String start) {
        List<District> districtList = districtService.list();
        districtList = districtList.stream().filter(d -> "Y".equals(d.getXxlyInfoSyn()))
                .collect(Collectors.toList());
        boolean isFirstTime = "0000".equals(code);

        log.info("手动触发【{}抽取处理新兴领域党建数据基础信息表数据V2】任务，当前时间: {}",
                isFirstTime ? "首次" : "", LocalDateTime.now());

        if (code != null && !isFirstTime) {
            District district = districtList.stream()
                    .filter(d -> code.equals(d.getCode()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("根据code没有匹配到对应的区！"));
            districtList = Collections.singletonList(district);
        }

        if (start != null) {
            isFirstTime = true;
        }

        for (District district : districtList) {
            log.info("开始处理【{}】信息表数据", district.getName());
            baseInfoV2Service.sourceToBase(district, isFirstTime);
        }

//        log.info("开始向目标表推送新兴领域党建数据基础信息");
//        baseInfoService.baseToTarget();
        log.info("任务执行完毕，当前时间: {}", LocalDateTime.now());
    }

    /**
     * 向目标表推送党建数据基础信息表数据
     */
    @GetMapping("/baseToTarget")
    public void baseToTarget() {
        log.info("开始向目标表推送新兴领域党建数据基础信息，当前时间: {}", LocalDateTime.now());
        baseInfoService.baseToTarget();
        log.info("推送新兴领域党建数据基础信息任务执行完毕，当前时间: {}", LocalDateTime.now());
    }

    /**
     * 向目标表推送党建数据基础信息表数据 dm
     */
    @GetMapping("/baseToTargetDm")
    public void baseToTargetDm() {
        log.info("开始向Dm目标表推送新兴领域党建数据基础信息，当前时间: {}", LocalDateTime.now());
        baseInfoService.baseToDmTarget();
        log.info("推送Dm新兴领域党建数据基础信息任务执行完毕，当前时间: {}", LocalDateTime.now());
    }

    /**
     * 向目标表推送党建数据基础信息表数据 dm
     */
    @GetMapping("/baseToTargetDmV2")
    public void baseToTargetDmV2() {
        log.info("开始向Dm目标表推送新兴领域党建数据基础信息V2，当前时间: {}", LocalDateTime.now());
        baseInfoV2Service.baseToDmTarget();
        log.info("推送Dm新兴领域党建数据基础信息V2任务执行完毕，当前时间: {}", LocalDateTime.now());
    }

    /**
     * 抽取各区新兴领域党建数据走访数据表，经过处理保存到市走访数据表
     * @param code 默认不传则处理全市各区，传区code处理指定区，传"0000"为首次处理
     */
    @GetMapping("/sourceToBaseDetail")
    public void sourceToBaseDetail(@RequestParam(value = "code", required = false) String code,
                                   @RequestParam(value = "start", required = false) String start,
                                   @RequestParam(value = "date", required = false) String date) {
        List<District> districtList = districtService.list();
        districtList = districtList.stream().filter(d -> "Y".equals(d.getXxlyZoufangSyn()))
                .collect(Collectors.toList());
        boolean isFirstTime = "0000".equals(code);

        log.info("手动触发【{}抽取处理新兴领域党建数据走访数据表】任务，当前时间: {}",
                isFirstTime ? "首次" : "", LocalDateTime.now());

        if (code != null && !isFirstTime) {
            District district = districtList.stream()
                    .filter(d -> code.equals(d.getCode()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("根据code没有匹配到对应的区！"));
            districtList = Collections.singletonList(district);
        }

        if (start != null) {
            isFirstTime = true;
        }

        for (District district : districtList) {
            log.info("开始处理【{}】走访数据表数据", district.getName());
            zfDetailService.sourceToBase(district, isFirstTime, date);
        }

//        log.info("开始向目标表推送新兴领域党建数据走访数据");
//        zfDetailService.baseToTarget();
        log.info("任务执行完毕，当前时间: {}", LocalDateTime.now());
    }

    /**
     * 向目标表推送党建走访数据
     */
    @GetMapping("/baseToTargetDetailDm")
    public void baseToTargetDetailDm() {
        log.info("开始向Dm目标表推送新兴领域党建走访数据，当前时间: {}", LocalDateTime.now());
        zfDetailService.baseToDmTarget();
        log.info("推送Dm新兴领域党建走访数据任务执行完毕，当前时间: {}", LocalDateTime.now());
    }

    /**
     * 抽取党建目标表的党组织数据
     */
    @GetMapping("/getDzzData")
    public void getDzzData() {
        log.info("开始从Dm目标表获取新兴领域党组织数据，当前时间: {}", LocalDateTime.now());
        dzzDetailService.getDzzData();
        log.info("从Dm目标表获取新兴领域党组织数据完成，当前时间: {}", LocalDateTime.now());
    }

    /**
     * 向各区推送党组织数据
     */
    @GetMapping("/pushDzzData")
    public void pushDzzData() {
        log.info("开始向各区推送新兴领域党组织数据，当前时间: {}", LocalDateTime.now());
        dzzDetailService.pushDzzData();
        log.info("推送新兴领域党组织数据完成，当前时间: {}", LocalDateTime.now());
    }



    /**
     * 备份基础V2表数据
     */
    @GetMapping("/backupV2")
    public void backupV2() {
        log.info("开始备份基础V2表数据，当前时间: {}", LocalDateTime.now());
        baseInfoV2Service.backupV2();
        log.info("备份基础V2表完毕，当前时间: {}", LocalDateTime.now());
    }

}