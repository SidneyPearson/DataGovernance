package com.dataGovernance.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dataGovernance.domain.entity.DwdAjWgajInfo1208;
import com.dataGovernance.domain.entity.DwdRlRecord;
import com.dataGovernance.domain.entity.GridConfig;
import com.dataGovernance.domain.entity.Rxb12345Gongdan06;
import com.dataGovernance.domain.entity.origin.DwdAjWgajInfo;
import com.dataGovernance.domain.entity.origin.SourceObj;
import com.dataGovernance.helper.SqlHelper;
import com.dataGovernance.helper.JdbcBatchInserter;
import com.dataGovernance.mapper.BaseInfoMapper;
import com.dataGovernance.service.BaseInfoService;
import com.dataGovernance.utils.DBConnectionUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dm.jdbc.filter.stat.json.JSONArray;
import dm.jdbc.filter.stat.json.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class BaseInfoServiceImpl extends ServiceImpl<BaseInfoMapper, Rxb12345Gongdan06>
        implements BaseInfoService {

    private final BaseInfoMapper baseInfoMapper;
    private final SqlHelper sqlHelper;
    private final JdbcBatchInserter jdbcBatchInserter;
    private final DataSource dataSource;

    private static final int PAGE_SIZE = 2000;
    private static final int BATCH_SIZE = 500;
    private static final String URL = "https://service-api.onemap.sh.cegn.cn/bigdata/realtime/v1.1/population";


    @Override
    public void sourceToBaseGongdan() {

        // 1. 统计要迁移的总数据量
        long totalToMigrate = sqlHelper.countFilteredRows();
        log.info("源表中满足条件的数据总量：{} 条（wp_type='投诉举报类'）", totalToMigrate);

        long start = System.currentTimeMillis();
        long total = 0;

        baseInfoMapper.truncateTable();
        log.info("目标表已清空");

        try {

            sqlHelper.openCursor();

            while (true) {

                List<SourceObj> batch = sqlHelper.fetchPage(PAGE_SIZE);
                if (batch.isEmpty()) break;

                List<Rxb12345Gongdan06> entities = batch.stream()
                        .map(this::convert)
                        .collect(Collectors.toList());

                try (Connection conn = dataSource.getConnection()) {
                    jdbcBatchInserter.insertBatch(conn, entities, BATCH_SIZE);
                }

                total += entities.size();

                if (total % 20000 == 0) {
                    log.info("已处理 {} 条", total);
                }
            }

        } catch (Exception e) {
            log.error("迁移失败", e);
        } finally {
            sqlHelper.closeCursor();
        }

        log.info("迁移完成，总 {} 条，耗时 {} 秒",
                total, (System.currentTimeMillis() - start) / 1000);


        try (Connection conn = dataSource.getConnection()) {
            sqlHelper.countResultRows(conn);
        } catch (Exception e) {
            log.error("执行查询失败", e);
        }

        log.info("执行查询完成！");
    }


    public void dwdTo1208() {

        long start = System.currentTimeMillis();
        long total = 0;

        log.info("开始迁移 dwd_aj_wgaj_info → dwd_aj_wgaj_info_1208");

        try {
            sqlHelper.openDwdCursor();
            while (true) {
                try (Connection conn = dataSource.getConnection()) {
                    List<DwdAjWgajInfo> page = sqlHelper.fetchDwdPage(PAGE_SIZE);
                    if (page.isEmpty()) break;

                    List<DwdAjWgajInfo1208> entities = page.stream()
                            .map(this::convertToDwd1208)
                            .collect(Collectors.toList());

                    jdbcBatchInserter.insertDwdBatch(conn, entities, BATCH_SIZE);
                    total += page.size();
                }

                if (total % 20000 == 0) {
                    log.info("已迁移 {} 条", total);
                }
            }

        } catch (Exception e) {
            log.error("迁移失败", e);
        } finally {
            sqlHelper.closeCursor();
        }

        log.info("迁移完成，共 {} 条，用时 {} 秒",
                total, (System.currentTimeMillis() - start) / 1000);
    }

    @Override
    public void syncRLRecord(GridConfig gridConfig) {

        long startTask = System.currentTimeMillis();
        log.info(">>>>>> 开始同步历史数据: {} ({}) <<<<<<", gridConfig.getGridName(), gridConfig.getApiId());

        // *****************************
        // 1. 处理时间范围 (如果没有传，给个默认值)
        // *****************************
        // 如果没传开始时间，默认跑昨天
        LocalDate startDay = gridConfig.getStartDate() != null ? gridConfig.getStartDate() : LocalDate.now().minusDays(1);
        // 如果没传结束时间，默认跑到今天
        LocalDate endDay = gridConfig.getEndDate() != null ? gridConfig.getEndDate() : LocalDate.now();

        log.info("同步时间范围: {} 到 {}", startDay, endDay);

        List<DwdRlRecord> buffer = new ArrayList<>();

        RestTemplate restTemplate = new RestTemplate();

        while (!startDay.isAfter(endDay)) {

            long begin = startDay.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
            long end = startDay.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toEpochSecond();

            // log.info("正在请求日期：{}", startDay); // 日志太多可以注释掉

            try {
                JSONObject body = new JSONObject();
                body.put("id", gridConfig.getApiId());
                body.put("begin", begin);
                body.put("end", end);
                body.put("interval", 60);
                body.put("people_type", "all");

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.add("jcmp-token", "6e70a7620bda40109c7f4066c6d9f240");

                HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);

                String resp = restTemplate.postForObject(
                        URL,
                        entity,
                        String.class
                );

                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(resp);
                JsonNode arr = root.path("data");

                for (JsonNode o : arr) {
                    String timeStr = o.get("time").asText();
                    int value = o.get("value").asInt();

                    LocalDateTime ldt = LocalDateTime.parse(timeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                    // 只要当天的数据 (剔除边界)
                    if (!ldt.toLocalDate().equals(startDay)) {
                        continue;
                    }

                    long ts = ldt.atZone(ZoneId.systemDefault()).toEpochSecond();

                    DwdRlRecord r = new DwdRlRecord();
                    // *****************************
                    // 使用配置对象中的信息填充
                    // *****************************
                    r.setGridCode(gridConfig.getGridCode());
                    r.setGridName(gridConfig.getGridName());
                    r.setStreet(gridConfig.getStreet());
                    r.setArea(gridConfig.getArea());

                    r.setBeginTime(ts);
                    r.setEndTime(ts + 3600);
                    r.setPopulation(value);

                    buffer.add(r);
                }

            } catch (Exception e) {
                log.error("调用接口失败 [日期:{}]", startDay, e);
            }

            // 内存保护：如果数据量太大(比如跑一年数据)，建议分批入库清空 buffer
            if (buffer.size() > 2000) {
                batchInsert(buffer); // 调用下方的入库方法
                buffer.clear();
            }

            startDay = startDay.plusDays(1);
        }

        // 处理剩余数据
        if (!buffer.isEmpty()) {
            batchInsert(buffer);
        }

        log.info("同步完成，总耗时 {} 秒", (System.currentTimeMillis() - startTask) / 1000);
    }

    private void batchInsert(List<DwdRlRecord> list) {
        try (Connection conn = DBConnectionUtils.getDMConnection("skyt_slxx_dghy_prod",
                "Skyt_slxx_dghy_prod@2025", "skyt_slxx_dghy_prod")) {
            // 假设 helper 已经注入或者是个静态工具
            jdbcBatchInserter.insertRlBatch(conn, list, 500);
            log.info("成功入库 {} 条记录", list.size());
        } catch (Exception e) {
            log.error("入库失败", e);
        }
    }


    private Rxb12345Gongdan06 convert(SourceObj s) {
        Rxb12345Gongdan06 e = new Rxb12345Gongdan06();

        e.setWpid(clean(s.getWpid()));
        e.setCalltime(parseDate(s.getCalltime()));
        e.setCallnum(clean(s.getCallnum()));
        e.setRelName(clean(s.getRelName()));
        e.setGender(clean(s.getGender()));
        e.setRelDistrict(clean(s.getRelDistrict()));
        e.setRelAddress(clean(s.getRelAddress()));
        e.setCallType(clean(s.getCallType()));
        e.setIsrepeat(clean(s.getIsrepeat()));
        e.setWpSource(clean(s.getWpSource()));
        e.setWpType(clean(s.getWpType()));
        e.setClass1(clean(s.getClass1()));
        e.setClass2(clean(s.getClass2()));
        e.setClass3(clean(s.getClass3()));
        e.setClass4(clean(s.getClass4()));
        e.setSummary(clean(s.getSummary()));
        e.setSupervision(clean(s.getSupervision()));
        e.setDeptLevel2(clean(s.getDeptLevel2()));
        e.setWpCustomertype(clean(s.getWpCustomertype()));
        e.setWpServicetype(clean(s.getWpServicetype()));
        e.setRelPhoneno(clean(s.getRelPhoneno()));
        e.setHurryCount(clean(s.getHurryCount()));
        e.setPriority(clean(s.getPriority()));
        e.setNote(clean(s.getNote()));
        e.setCallid(clean(s.getCallid()));
        e.setNewClass1(clean(s.getNewClass1()));
        e.setNewClass2(clean(s.getNewClass2()));
        e.setNewClass3(clean(s.getNewClass3()));
        e.setNewClass4(clean(s.getNewClass4()));
        e.setNewClass5(clean(s.getNewClass5()));
        e.setDeptLevel3(clean(s.getDeptLevel3()));
        e.setCreateTime(parseDate(s.getCreateTime()));
        e.setDsjzxTaskid(clean(s.getDsjzxTaskid()));

        return e;
    }

    private DwdAjWgajInfo1208 convertToDwd1208(DwdAjWgajInfo s) {

        DwdAjWgajInfo1208 e = new DwdAjWgajInfo1208();

        e.setVerifyImg(clean(s.getVerifyImg()));
        e.setReportImg(clean(s.getReportImg()));
        e.setContactPhone(clean(s.getContactPhone()));
        e.setReporter(clean(s.getReporter()));
        e.setCaseType(clean(s.getCaseType()));
        e.setIssueSrc(clean(s.getIssueSrc()));
        e.setProblemDescription(clean(s.getProblemDescription()));

        e.setYCoordinate(clean(s.getYCoordinate()));
        e.setTaskId(clean(s.getTaskId()));
        e.setGridCode(clean(s.getGridCode()));
        e.setStreetName(clean(s.getStreetName()));
        e.setVallageCommunity(clean(s.getVallageCommunity()));
        e.setDiscoverTime(clean(s.getDiscoverTime()));
        e.setCaseStatus(clean(s.getCaseStatus()));
        e.setSettleTime(clean(s.getSettleTime()));
        e.setOccurrenceAddress(clean(s.getOccurrenceAddress()));
        e.setXCoordinate(clean(s.getXCoordinate()));

        e.setInserttime(parseDate(s.getInserttime()));
        e.setUpdatetime(parseDate(s.getUpdatetime()));

        e.setAreaCode(clean(s.getAreaCode()));
        e.setArea(clean(s.getArea()));
        e.setStreetCode(clean(s.getStreetCode()));
        e.setGridName(clean(s.getGridName()));
        e.setStreet(clean(s.getStreet()));

        e.setDiscoverTimeTs(parseDate(s.getDiscoverTimeTs()));
        e.setSourceArea(clean(s.getSourceArea()));
        e.setMergeTime(parseDate(s.getMergeTime()));

        e.setQl(s.getQl() != null ? Integer.parseInt(s.getQl()) : 0);
        e.setQw(s.getQw() != null ? Integer.parseInt(s.getQw()) : 0);
        e.setZd(s.getZd() != null ? Integer.parseInt(s.getZd()) : 0);

        e.setWpid(clean(s.getWpid()));
        e.setWpType(clean(s.getWpType()));
        e.setDiscoverTimeYm(clean(s.getDiscoverTimeYm()));

        return e;
    }

    private String clean(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty() || "null".equalsIgnoreCase(t)) return null;
        return t;
    }

    private Date parseDate(String str) {
        if (str == null) return null;
        str = str.trim();
        if (str.isEmpty() || "null".equalsIgnoreCase(str)) return null;

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);
            return sdf.parse(str);
        } catch (Exception ignore) {}

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return sdf.parse(str);
        } catch (Exception ignore) {}

        try {
            return Timestamp.valueOf(str);
        } catch (Exception ignore) {}

        return null;
    }


}
