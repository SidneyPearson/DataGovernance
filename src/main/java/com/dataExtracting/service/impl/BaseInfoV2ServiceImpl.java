package com.dataExtracting.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dataExtracting.domain.constant.ProcessStats;
import com.dataExtracting.domain.entity.*;
import com.dataExtracting.domain.enums.DistrictEnum;
import com.dataExtracting.helper.SqlHelper;
import com.dataExtracting.mapper.BaseInfoV2CopyMapper;
import com.dataExtracting.mapper.BaseInfoV2Mapper;
import com.dataExtracting.mapper.CityGridMapper;
import com.dataExtracting.service.BaseInfoV2Service;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
public class BaseInfoV2ServiceImpl extends ServiceImpl<BaseInfoV2Mapper, BaseInfoV2>
        implements BaseInfoV2Service {

    @Autowired
    private SqlHelper sqlHelper;
    @Autowired
    private CityGridMapper cityGridMapper;
    @Autowired
    private BaseInfoV2CopyMapper baseInfoV2CopyMapper;

    private static final Logger log = LoggerFactory.getLogger(BaseInfoV2ServiceImpl.class);

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sourceToBase(District district, boolean isFirst) {
        String todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        DistrictEnum districtEnum = DistrictEnum.fromName(district.getName());

        List<SourceObjV2> sourceObjs = sqlHelper.getSourceInfoV2Data(
                districtEnum.getUsername(),
                districtEnum.getPassword(),
                districtEnum.getSchema(),
                isFirst ? null : todayStr
        );
        log.info("获取源数据数量: 【{}】", sourceObjs.size());

        // 按 gridCode 分组，每组保留 updateTime 最大（最新）的一条
        Map<String, SourceObjV2> latestRecords = sourceObjs.stream()
                .collect(Collectors.toMap(
                        SourceObjV2::getGridCode,
                        Function.identity(),
                        (oldObj, newObj) -> oldObj.getJhptUpdateTime()
                                .compareTo(newObj.getJhptUpdateTime()) > 0 ? oldObj : newObj
                ));

        sourceObjs = new ArrayList<>(latestRecords.values());
        log.info("去重后数据数量: 【{}】", sourceObjs.size());

        ProcessStats stats = new ProcessStats();

        if (isFirst) {
            sourceObjs.stream()
                    .filter(source -> StringUtils.isNotEmpty(source.getGridCode()))
                    .forEach(source -> processSourceData(source, district, stats));
        } else {
            sourceObjs.stream()
                    .filter(source -> StringUtils.isNotEmpty(source.getGridCode()))
                    .forEach(source -> processInsertOrUpdateBaseData(source, district, stats));
        }

        log.info("数据处理统计 - 总数: {}, 新增: {}, 更新: {}",
                stats.getTotal(), stats.getInserted(), stats.getUpdated());

    }

    private void processSourceData(SourceObjV2 source, District district, ProcessStats stats) {
        // *各区的来源表中可能存在GridCode和SourceArea相同的数据
        // 检查是否已存在 如果存在则拿出这条数据的JhptUpdateTime和来源数据的做对比，如果来源数据的时间更新点，则做更新
        stats.incrementTotal();
        boolean exists = count(new LambdaQueryWrapper<BaseInfoV2>()
                .eq(BaseInfoV2::getGridCode, source.getGridCode())
                .eq(BaseInfoV2::getSourceArea, district.getCode())) > 0;

        if (exists) {
            LambdaQueryWrapper<BaseInfoV2> queryWrapper = new LambdaQueryWrapper<BaseInfoV2>()
                    .eq(BaseInfoV2::getGridCode, source.getGridCode())
                    .eq(BaseInfoV2::getSourceArea, district.getCode());
            BaseInfoV2 baseInfoV2 = getOne(queryWrapper);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            try {
                if (baseInfoV2.getJhptUpdateTime().before(sdf.parse(source.getJhptUpdateTime()))) {
                    BeanUtil.copyProperties(source, baseInfoV2, CopyOptions.create()
                            .setIgnoreNullValue(true) // 忽略源对象null值
                            .setIgnoreError(true));   // 忽略类型转换错误
                    baseInfoV2.setUpdateTime(new Date());
                    update(baseInfoV2, queryWrapper); // 使用相同条件更新
                    stats.incrementUpdated();
                }
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        } else {
            // 创建新记录
            BaseInfoV2 BaseInfoV2 = new BaseInfoV2();
            BeanUtil.copyProperties(source, BaseInfoV2, true);
            BaseInfoV2.setSourceArea(district.getCode());
            BaseInfoV2.setAreaName(district.getName());
            BaseInfoV2.setUpdateTime(new Date());

            // 补充网格信息
            CityGrid cityGrid = cityGridMapper.selectOne(
                    new LambdaQueryWrapper<CityGrid>()
                            .eq(CityGrid::getGridCode, source.getGridCode()));
            if (cityGrid == null) {
                log.info("网格编码不存在: {}", source.getGridCode());
                return;
            } else if (!cityGrid.getDistrictName().equals(district.getName())) {
                log.info("网格编码 {} 与区划 {} 不匹配", source.getGridCode(), district.getName());
                return;
            }

            BaseInfoV2.setStreetCode(cityGrid.getStreetCode());
            BaseInfoV2.setStreetName(cityGrid.getStreetName());
            BaseInfoV2.setWgAreaCode(cityGrid.getAreaCode());
            BaseInfoV2.setWgAreaName(cityGrid.getDistrictName());

            save(BaseInfoV2);
            stats.incrementInserted();
        }
    }


    private void processInsertOrUpdateBaseData(SourceObjV2 source, District district, ProcessStats stats) {
        stats.incrementTotal();
        // 1. 构建唯一条件
        String gridCode = source.getGridCode();
        String sourceArea = district.getCode();

        // 2. 尝试获取已有记录（使用联合唯一键）
        LambdaQueryWrapper<BaseInfoV2> queryWrapper = new LambdaQueryWrapper<BaseInfoV2>()
                .eq(BaseInfoV2::getGridCode, gridCode)
                .eq(BaseInfoV2::getSourceArea, sourceArea);

        BaseInfoV2 existing = getOne(queryWrapper);

        if (existing != null) {
            // 3. 更新操作（保留原值策略）
            BeanUtil.copyProperties(source, existing, CopyOptions.create()
                    .setIgnoreNullValue(true) // 忽略源对象null值
                    .setIgnoreError(true));   // 忽略类型转换错误
            existing.setUpdateTime(new Date());
            update(existing, queryWrapper); // 使用相同条件更新
            stats.incrementUpdated();
        } else {
            // 4. 插入新记录
            CityGrid cityGrid = cityGridMapper.selectOne(
                    new LambdaQueryWrapper<CityGrid>()
                            .eq(CityGrid::getGridCode, gridCode));

            if (cityGrid == null) {
                log.info("网格编码不存在: {}", gridCode);
                return;
            } else if (!cityGrid.getDistrictName().equals(district.getName())) {
                log.info("网格编码 {} 与区划 {} 不匹配", source.getGridCode(), district.getName());
                return;
            }

            BaseInfoV2 newRecord = new BaseInfoV2();
            BeanUtil.copyProperties(source, newRecord);
            newRecord.setSourceArea(sourceArea);
            newRecord.setAreaName(district.getName());
            newRecord.setUpdateTime(new Date());
            newRecord.setStreetCode(cityGrid.getStreetCode());
            newRecord.setStreetName(cityGrid.getStreetName());
            newRecord.setWgAreaCode(cityGrid.getAreaCode());
            newRecord.setWgAreaName(cityGrid.getDistrictName());

            save(newRecord);
            stats.incrementInserted();
        }
    }

    @Override
    public void baseToDmTarget() {
        LambdaQueryWrapper<BaseInfoV2> queryWrapper = new LambdaQueryWrapper<>();
        // 获取上次同步的最大更新时间
//        Date lastSyncTime = sqlHelper.getLastSyncTimeFromDmTargetV2();
//        if (lastSyncTime != null) {
//            queryWrapper.gt(BaseInfoV2::getUpdateTime, lastSyncTime);
//        }

//        log.info("lastSyncTime:{} ", lastSyncTime);

        // 分页查询源库
        int pageSize = 1000;
        int current = 1;
        int totalMigrated = 0;
        boolean hasMoreData = true;

        Connection targetConn = sqlHelper.getDmTargetConn();
        try {
            targetConn.setAutoCommit(false);
            String insertSql = "INSERT INTO dwd_dj_xxly_info_v2 (" +
                    "grid_code, grid_name, sczt, cyry, fgsczt, jq, ly, yq, sq, " +
                    "sqsc, cun, qt, ddzjdzz, lhzjdzz, lydzz, yqdzz, jqdzz, sqscdzz, " +
                    "cdzz, hyzz, lsdzz, wfg, ddzjdzzry, lhzjdzzry, lydzzry, yqdzzry, " +
                    "jqdzzry, sqscdzzry, cdzzry, hyzzry, lsdzzry, wfgry, zgmyqy500qwc, " +
                    "zgmyqy500q, zghlwqy100qwc, zghlwqy100q, zjtxxjrqywc, zjtxxjrqy, " +
                    "jnzbssqywc, jnzbssqy, djsqywc, djsqy, kjxzxqywc, kjxzxqy, " +
                    "jymbfqydwwc, jymbfqydw, ylwsjgmbfqydwwc, ylwsjgmbfqydw, cpjrjgwc, " +
                    "cpjrjg, hlwqywc, hlwqy, dykjzxqywc, dykjzxqy, source_area, " +
                    "area_name, street_code, street_name, wg_area_code, wg_area_name, " +
                    "process_state, jhpt_update_time, dsjzx_taskid" +
                    ") VALUES (" +
                    "?, ?, ?, ?, ?, ?, ?, ?, ?, " +  // 1-9
                    "?, ?, ?, ?, ?, ?, ?, ?, ?, " +  // 10-18
                    "?, ?, ?, ?, ?, ?, ?, ?, ?, " +  // 19-27
                    "?, ?, ?, ?, ?, ?, ?, ?, ?, " +  // 28-36
                    "?, ?, ?, ?, ?, ?, ?, ?, ?, " +  // 37-45
                    "?, ?, ?, ?, ?, ?, ?, ?, ?, " +  // 46-54
                    "?, ?, ?, ?, ?, ?, ?, ?, ?)";    // 55-63

            String updateSql = "UPDATE dwd_dj_xxly_info_v2 SET " +
                    "grid_name = ?, sczt = ?, cyry = ?, fgsczt = ?, jq = ?, ly = ?, yq = ?, sq = ?, " +  // 2-9
                    "sqsc = ?, cun = ?, qt = ?, ddzjdzz = ?, lhzjdzz = ?, lydzz = ?, yqdzz = ?, jqdzz = ?, sqscdzz = ?, " +  // 10-18
                    "cdzz = ?, hyzz = ?, lsdzz = ?, wfg = ?, ddzjdzzry = ?, lhzjdzzry = ?, lydzzry = ?, yqdzzry = ?, " +  // 19-26
                    "jqdzzry = ?, sqscdzzry = ?, cdzzry = ?, hyzzry = ?, lsdzzry = ?, wfgry = ?, zgmyqy500qwc = ?, " +  // 27-33
                    "zgmyqy500q = ?, zghlwqy100qwc = ?, zghlwqy100q = ?, zjtxxjrqywc = ?, zjtxxjrqy = ?, jnzbssqywc = ?, " +  // 34-40
                    "jnzbssqy = ?, djsqywc = ?, djsqy = ?, kjxzxqywc = ?, kjxzxqy = ?, jymbfqydwwc = ?, jymbfqydw = ?, " +  // 41-47
                    "ylwsjgmbfqydwwc = ?, ylwsjgmbfqydw = ?, cpjrjgwc = ?, cpjrjg = ?, hlwqywc = ?, hlwqy = ?, " +  // 48-54
                    "dykjzxqywc = ?, dykjzxqy = ?, area_name = ?, street_code = ?, street_name = ?, " +  // 55-59
                    "wg_area_code = ?, wg_area_name = ?, process_state = ?, jhpt_update_time = ?, dsjzx_taskid = ? " +  // 60-64
                    "WHERE source_area = ? AND grid_code = ?";  // 66-67（条件）

            while (hasMoreData) {
                Page<BaseInfoV2> page = new Page<>(current, pageSize);
                IPage<BaseInfoV2> pageResult = baseMapper.selectPage(page, queryWrapper);
                List<BaseInfoV2> records = pageResult.getRecords();

                List<BaseInfoV2> insertList = new ArrayList<>();
                List<BaseInfoV2> updateList = new ArrayList<>();

                // 终止条件：当前页无数据或已到达最后一页
                if (records.isEmpty() || records.size() < pageSize) {
                    hasMoreData = false;
                }

                if (!records.isEmpty()) {
                    for (BaseInfoV2 record : records) {
                        boolean exists = checkIfRecordExists(targetConn, record.getSourceArea(), record.getGridCode());
                        if (exists) {
                            updateList.add(record);
                        } else {
                            insertList.add(record);
                        }
                    }

                    // 批量插入
                    try (PreparedStatement pstmt = targetConn.prepareStatement(insertSql)) {
                        for (BaseInfoV2 info : insertList) {
                            int paramIndex = 1;

                            // 设置参数
                            pstmt.setString(paramIndex++, info.getGridCode());
                            pstmt.setString(paramIndex++, info.getGridName());

                            // 所有整型字段
                            setNullableInt(pstmt, paramIndex++, info.getSczt());
                            setNullableInt(pstmt, paramIndex++, info.getCyry());
                            setNullableInt(pstmt, paramIndex++, info.getFgsczt());
                            setNullableInt(pstmt, paramIndex++, info.getJq());
                            setNullableInt(pstmt, paramIndex++, info.getLy());
                            setNullableInt(pstmt, paramIndex++, info.getYq());
                            setNullableInt(pstmt, paramIndex++, info.getSq());
                            setNullableInt(pstmt, paramIndex++, info.getSqsc());
                            setNullableInt(pstmt, paramIndex++, info.getCun());
                            setNullableInt(pstmt, paramIndex++, info.getQt());
                            setNullableInt(pstmt, paramIndex++, info.getDdzjdzz());
                            setNullableInt(pstmt, paramIndex++, info.getLhzjdzz());
                            setNullableInt(pstmt, paramIndex++, info.getLydzz());
                            setNullableInt(pstmt, paramIndex++, info.getYqdzz());
                            setNullableInt(pstmt, paramIndex++, info.getJqdzz());
                            setNullableInt(pstmt, paramIndex++, info.getSqscdzz());
                            setNullableInt(pstmt, paramIndex++, info.getCdzz());
                            setNullableInt(pstmt, paramIndex++, info.getHyzz());
                            setNullableInt(pstmt, paramIndex++, info.getLsdzz());
                            setNullableInt(pstmt, paramIndex++, info.getWfg());
                            setNullableInt(pstmt, paramIndex++, info.getDdzjdzzry());
                            setNullableInt(pstmt, paramIndex++, info.getLhzjdzzry());
                            setNullableInt(pstmt, paramIndex++, info.getLydzzry());
                            setNullableInt(pstmt, paramIndex++, info.getYqdzzry());
                            setNullableInt(pstmt, paramIndex++, info.getJqdzzry());
                            setNullableInt(pstmt, paramIndex++, info.getSqscdzzry());
                            setNullableInt(pstmt, paramIndex++, info.getCdzzry());
                            setNullableInt(pstmt, paramIndex++, info.getHyzzry());
                            setNullableInt(pstmt, paramIndex++, info.getLsdzzry());
                            setNullableInt(pstmt, paramIndex++, info.getWfgry());
                            setNullableInt(pstmt, paramIndex++, info.getZgmyqy500qwc());
                            setNullableInt(pstmt, paramIndex++, info.getZgmyqy500q());
                            setNullableInt(pstmt, paramIndex++, info.getZghlwqy100qwc());
                            setNullableInt(pstmt, paramIndex++, info.getZghlwqy100q());
                            setNullableInt(pstmt, paramIndex++, info.getZjtxxjrqywc());
                            setNullableInt(pstmt, paramIndex++, info.getZjtxxjrqy());
                            setNullableInt(pstmt, paramIndex++, info.getJnzbssqywc());
                            setNullableInt(pstmt, paramIndex++, info.getJnzbssqy());
                            setNullableInt(pstmt, paramIndex++, info.getDjsqywc());
                            setNullableInt(pstmt, paramIndex++, info.getDjsqy());
                            setNullableInt(pstmt, paramIndex++, info.getKjxzxqywc());
                            setNullableInt(pstmt, paramIndex++, info.getKjxzxqy());
                            setNullableInt(pstmt, paramIndex++, info.getJymbfqydwwc());
                            setNullableInt(pstmt, paramIndex++, info.getJymbfqydw());
                            setNullableInt(pstmt, paramIndex++, info.getYlwsjgmbfqydwwc());
                            setNullableInt(pstmt, paramIndex++, info.getYlwsjgmbfqydw());
                            setNullableInt(pstmt, paramIndex++, info.getCpjrjgwc());
                            setNullableInt(pstmt, paramIndex++, info.getCpjrjg());
                            setNullableInt(pstmt, paramIndex++, info.getHlwqywc());
                            setNullableInt(pstmt, paramIndex++, info.getHlwqy());
                            setNullableInt(pstmt, paramIndex++, info.getDykjzxqywc());
                            setNullableInt(pstmt, paramIndex++, info.getDykjzxqy());

                            // 字符串和日期字段
                            pstmt.setString(paramIndex++, info.getSourceArea());
                            pstmt.setString(paramIndex++, info.getAreaName());
                            pstmt.setString(paramIndex++, info.getStreetCode());
                            pstmt.setString(paramIndex++, info.getStreetName());
                            pstmt.setString(paramIndex++, info.getWgAreaCode());
                            pstmt.setString(paramIndex++, info.getWgAreaName());
//                            pstmt.setString(paramIndex++, info.getProcessState());
                            pstmt.setString(paramIndex++, "0");
                            pstmt.setTimestamp(paramIndex++, new Timestamp(info.getJhptUpdateTime().getTime()));
                            pstmt.setString(paramIndex++, info.getUpdateTime().toInstant()
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")));

                            pstmt.addBatch();
                        }
                        pstmt.executeBatch();
                        targetConn.commit();
                        totalMigrated += insertList.size();
                        log.info("已插入{}条数据，当前进度: {}/{}",
                                insertList.size(), totalMigrated, pageResult.getTotal());
                    }

                    // 批量更新
                    try (PreparedStatement pstmt = targetConn.prepareStatement(updateSql)) {
                        for (BaseInfoV2 info : updateList) {
                            int paramIndex = 1;

                            // SET 部分
                            pstmt.setString(paramIndex++, info.getGridName());
                            setNullableInt(pstmt, paramIndex++, info.getSczt());
                            setNullableInt(pstmt, paramIndex++, info.getCyry());
                            setNullableInt(pstmt, paramIndex++, info.getFgsczt());
                            setNullableInt(pstmt, paramIndex++, info.getJq());
                            setNullableInt(pstmt, paramIndex++, info.getLy());
                            setNullableInt(pstmt, paramIndex++, info.getYq());
                            setNullableInt(pstmt, paramIndex++, info.getSq());
                            setNullableInt(pstmt, paramIndex++, info.getSqsc());
                            setNullableInt(pstmt, paramIndex++, info.getCun());
                            setNullableInt(pstmt, paramIndex++, info.getQt());
                            setNullableInt(pstmt, paramIndex++, info.getDdzjdzz());
                            setNullableInt(pstmt, paramIndex++, info.getLhzjdzz());
                            setNullableInt(pstmt, paramIndex++, info.getLydzz());
                            setNullableInt(pstmt, paramIndex++, info.getYqdzz());
                            setNullableInt(pstmt, paramIndex++, info.getJqdzz());
                            setNullableInt(pstmt, paramIndex++, info.getSqscdzz());
                            setNullableInt(pstmt, paramIndex++, info.getCdzz());
                            setNullableInt(pstmt, paramIndex++, info.getHyzz());
                            setNullableInt(pstmt, paramIndex++, info.getLsdzz());
                            setNullableInt(pstmt, paramIndex++, info.getWfg());
                            setNullableInt(pstmt, paramIndex++, info.getDdzjdzzry());
                            setNullableInt(pstmt, paramIndex++, info.getLhzjdzzry());
                            setNullableInt(pstmt, paramIndex++, info.getLydzzry());
                            setNullableInt(pstmt, paramIndex++, info.getYqdzzry());
                            setNullableInt(pstmt, paramIndex++, info.getJqdzzry());
                            setNullableInt(pstmt, paramIndex++, info.getSqscdzzry());
                            setNullableInt(pstmt, paramIndex++, info.getCdzzry());
                            setNullableInt(pstmt, paramIndex++, info.getHyzzry());
                            setNullableInt(pstmt, paramIndex++, info.getLsdzzry());
                            setNullableInt(pstmt, paramIndex++, info.getWfgry());
                            setNullableInt(pstmt, paramIndex++, info.getZgmyqy500qwc());
                            setNullableInt(pstmt, paramIndex++, info.getZgmyqy500q());
                            setNullableInt(pstmt, paramIndex++, info.getZghlwqy100qwc());
                            setNullableInt(pstmt, paramIndex++, info.getZghlwqy100q());
                            setNullableInt(pstmt, paramIndex++, info.getZjtxxjrqywc());
                            setNullableInt(pstmt, paramIndex++, info.getZjtxxjrqy());
                            setNullableInt(pstmt, paramIndex++, info.getJnzbssqywc());
                            setNullableInt(pstmt, paramIndex++, info.getJnzbssqy());
                            setNullableInt(pstmt, paramIndex++, info.getDjsqywc());
                            setNullableInt(pstmt, paramIndex++, info.getDjsqy());
                            setNullableInt(pstmt, paramIndex++, info.getKjxzxqywc());
                            setNullableInt(pstmt, paramIndex++, info.getKjxzxqy());
                            setNullableInt(pstmt, paramIndex++, info.getJymbfqydwwc());
                            setNullableInt(pstmt, paramIndex++, info.getJymbfqydw());
                            setNullableInt(pstmt, paramIndex++, info.getYlwsjgmbfqydwwc());
                            setNullableInt(pstmt, paramIndex++, info.getYlwsjgmbfqydw());
                            setNullableInt(pstmt, paramIndex++, info.getCpjrjgwc());
                            setNullableInt(pstmt, paramIndex++, info.getCpjrjg());
                            setNullableInt(pstmt, paramIndex++, info.getHlwqywc());
                            setNullableInt(pstmt, paramIndex++, info.getHlwqy());
                            setNullableInt(pstmt, paramIndex++, info.getDykjzxqywc());
                            setNullableInt(pstmt, paramIndex++, info.getDykjzxqy());

                            // 字符串和日期字段
                            pstmt.setString(paramIndex++, info.getAreaName());
                            pstmt.setString(paramIndex++, info.getStreetCode());
                            pstmt.setString(paramIndex++, info.getStreetName());
                            pstmt.setString(paramIndex++, info.getWgAreaCode());
                            pstmt.setString(paramIndex++, info.getWgAreaName());
//                            pstmt.setString(paramIndex++, info.getProcessState());
                            pstmt.setString(paramIndex++, "0");
                            pstmt.setTimestamp(paramIndex++, new Timestamp(info.getJhptUpdateTime().getTime()));
                            pstmt.setString(paramIndex++, info.getUpdateTime().toInstant()
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")));

                            // WHERE 条件
                            pstmt.setString(paramIndex++, info.getSourceArea());
                            pstmt.setString(paramIndex++, info.getGridCode());

                            pstmt.addBatch();
                        }
                        pstmt.executeBatch();
                        targetConn.commit();
                        totalMigrated += updateList.size();
                        log.info("已更新{}条数据，当前进度: {}/{}",
                                updateList.size(), totalMigrated, pageResult.getTotal());
                    }
                }
                current++;
            }
        } catch (SQLException e) {
            throw new RuntimeException("跨库数据库数据推送失败", e);
        }

        log.info("数据推送完成，共推送了{}条数据", totalMigrated);
    }

    @Override
    public void backupV2() {
//        baseInfoV2CopyMapper.delete(null);
        for (BaseInfoV2 baseInfoV2 : list()) {
            BaseInfoV2Copy baseInfoV2Copy = new BaseInfoV2Copy();
            BeanUtil.copyProperties(baseInfoV2, baseInfoV2Copy, true);
            baseInfoV2Copy.setBackupTime(new Date());
            baseInfoV2CopyMapper.insert(baseInfoV2Copy);
        }
    }


    private void setNullableInt(PreparedStatement pstmt, int paramIndex, Integer value) throws SQLException {
        if (value != null) {
            pstmt.setInt(paramIndex, value);
        } else {
            pstmt.setNull(paramIndex, Types.INTEGER);
        }
    }

    private boolean checkIfRecordExists(Connection conn, String sourceArea, String gridCode) throws SQLException {
        String sql = "SELECT 1 FROM dwd_dj_xxly_info_v2 WHERE source_area = ? AND grid_code = ? LIMIT 1";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, sourceArea);
            pstmt.setString(2, gridCode);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next(); // 如果有数据返回 true，否则 false
            }
        }
    }


}
