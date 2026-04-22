package com.dataGovernance.controller;

import com.dataGovernance.domain.entity.AnalyzeRangeReq;
import com.dataGovernance.domain.entity.GridConfig;
import com.dataGovernance.service.BaseInfoService;
import com.dataGovernance.service.DataAnalyzeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/data")
public class DataProcessController extends BaseController{

    @Autowired
    private BaseInfoService baseInfoService;

    @Autowired
    private DataAnalyzeService dataAnalyzeService;

    private static final Logger log = LoggerFactory.getLogger(DataProcessController.class);

    @GetMapping("/sourceToBaseGongdan")
    public void sourceToBaseGongdan() {
        log.info("手动触发【抽取处理12345工单数据表】任务，当前时间: {}", LocalDateTime.now());

        baseInfoService.sourceToBaseGongdan();

        log.info("任务执行完毕，当前时间: {}", LocalDateTime.now());
    }

    @GetMapping("/migrateDwdAjWgajInfo")
    public void migrateDwdAjWgajInfo() {
        log.info("手动触发【抽取处理DwdAjWgajInfo数据表】任务，当前时间: {}", LocalDateTime.now());

        baseInfoService.dwdTo1208();

        log.info("任务执行完毕，当前时间: {}", LocalDateTime.now());
    }

    @GetMapping("/syncRLRecord")
    public void syncRLRecord() {
        log.info("手动触发【获取人流记录】任务，当前时间: {}", LocalDateTime.now());

        log.info("手动触发【获取人流记录】任务...");

        // 1. 定义要跑的时间段
        LocalDate start = LocalDate.of(2026, 1, 30);
        LocalDate end = LocalDate.now();

        // 2. 定义网格列表 (以后可以配置在数据库或配置文件里)
        List<GridConfig> grids = Arrays.asList(
                new GridConfig("4151772507108546469", "ZH31010101301", "第一综合网格", "外滩街道", "黄浦区", start, end),
                new GridConfig("4151772519818439747", "ZH31010101302", "第二综合网格", "外滩街道", "黄浦区", start, end),
                new GridConfig("4151772520350577084", "ZH31010101303", "第三综合网格", "外滩街道", "黄浦区", start, end)
        );

        // 3. 异步执行 (关键！)
        // 因为同步3个网格的数据可能耗时很久，如果直接在主线程跑，浏览器会一直转圈直到超时。
        // 我们启动一个新线程在后台跑，立刻给浏览器返回“成功”。
        new Thread(() -> {
            for (GridConfig grid : grids) {
                try {
                    baseInfoService.syncRLRecord(grid);
                } catch (Exception e) {
                    log.error("网格 [{}] 同步失败", grid.getGridName(), e);
                }
            }
            log.info("所有网格同步任务执行完毕！");
        }).start();

        log.info("任务执行完毕，当前时间: {}", LocalDateTime.now());
    }




    @PostMapping("/range")
    public String analyzeByRange(@RequestBody AnalyzeRangeReq req) {
        dataAnalyzeService.analyzeRange(
                req.getStartTime(),
                req.getEndTime(),
                req.getGridCodes()
        );
        return "分析任务已触发";
    }


}