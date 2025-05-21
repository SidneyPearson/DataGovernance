package com.dataExtracting.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dataExtracting.domain.entity.BaseInfo;
import com.dataExtracting.domain.entity.CityGrid;
import com.dataExtracting.domain.entity.District;
import com.dataExtracting.domain.entity.SourceObj;
import com.dataExtracting.domain.enums.DistrictEnum;
import com.dataExtracting.helper.SqlHelper;
import com.dataExtracting.mapper.BaseInfoMapper;
import com.dataExtracting.mapper.CityGridMapper;
import com.dataExtracting.service.BaseInfoService;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Service
public class BaseInfoServiceImpl extends ServiceImpl<BaseInfoMapper, BaseInfo>
        implements BaseInfoService {

    @Autowired
    private SqlHelper sqlHelper;
    @Autowired
    private CityGridMapper cityGridMapper;

    private static final Logger log = LoggerFactory.getLogger(BaseInfoServiceImpl.class);

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sourceToBase(District district, boolean isFirst) {
        String todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        DistrictEnum districtEnum = DistrictEnum.fromName(district.getName());

        // 获取源数据
        List<SourceObj> sourceObjs = sqlHelper.getSourceInfoData(
                districtEnum.getUsername(),
                districtEnum.getPassword(),
                districtEnum.getSchema(),
                isFirst ? null : todayStr
        );
        log.info("获取源数据数量: 【{}】", sourceObjs.size());

        if (isFirst) {
            sourceObjs.stream()
                    .filter(source -> StringUtils.isNotEmpty(source.getGridCode()))
                    .forEach(source -> processSourceData(source, district));
        } else {
            sourceObjs.stream()
                    .filter(source -> StringUtils.isNotEmpty(source.getGridCode()))
                    .forEach(source -> processInsertOrUpdateBaseData(source, district));
        }

    }

    private void processSourceData(SourceObj source, District district) {
        // *各区的来源表中可能存在GridCode和SourceArea相同的数据
        // 检查是否已存在 如果存在则拿出这条数据的JhptUpdateTime和来源数据的做对比，如果来源数据的时间更新点，则做更新
        boolean exists = count(new LambdaQueryWrapper<BaseInfo>()
                .eq(BaseInfo::getGridCode, source.getGridCode())
                .eq(BaseInfo::getSourceArea, district.getCode())) > 0;

        if (exists) {
            LambdaQueryWrapper<BaseInfo> queryWrapper = new LambdaQueryWrapper<BaseInfo>()
                    .eq(BaseInfo::getGridCode, source.getGridCode())
                    .eq(BaseInfo::getSourceArea, district.getCode());
            BaseInfo baseInfo = getOne(queryWrapper);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            try {
                if (baseInfo.getJhptUpdateTime().before(sdf.parse(source.getJhptUpdateTime()))) {
                    BeanUtil.copyProperties(source, baseInfo, CopyOptions.create()
                            .setIgnoreNullValue(true) // 忽略源对象null值
                            .setIgnoreError(true));   // 忽略类型转换错误
                    baseInfo.setUpdateTime(new Date());
                    update(baseInfo, queryWrapper); // 使用相同条件更新
                }
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        } else {
            // 创建新记录
            BaseInfo baseInfo = new BaseInfo();
            BeanUtil.copyProperties(source, baseInfo, true);
            baseInfo.setSourceArea(district.getCode());
            baseInfo.setAreaName(district.getName());
            baseInfo.setUpdateTime(new Date());

            // 补充网格信息
            CityGrid cityGrid = cityGridMapper.selectOne(
                    new LambdaQueryWrapper<CityGrid>()
                            .eq(CityGrid::getGridCode, source.getGridCode()));
            if (cityGrid == null) {
                log.debug("网格编码不存在: {}", source.getGridCode());
                return;
            }

            baseInfo.setStreetCode(cityGrid.getStreetCode());
            baseInfo.setStreetName(cityGrid.getStreetName());
            baseInfo.setWgAreaCode(cityGrid.getAreaCode());
            baseInfo.setWgAreaName(cityGrid.getDistrictName());

            save(baseInfo);
        }
    }


    private void processInsertOrUpdateBaseData(SourceObj source, District district) {
        // 1. 构建唯一条件
        String gridCode = source.getGridCode();
        String sourceArea = district.getCode();

        // 2. 尝试获取已有记录（使用联合唯一键）
        LambdaQueryWrapper<BaseInfo> queryWrapper = new LambdaQueryWrapper<BaseInfo>()
                .eq(BaseInfo::getGridCode, gridCode)
                .eq(BaseInfo::getSourceArea, sourceArea);

        BaseInfo existing = getOne(queryWrapper);

        if (existing != null) {
            // 3. 更新操作（保留原值策略）
            BeanUtil.copyProperties(source, existing, CopyOptions.create()
                    .setIgnoreNullValue(true) // 忽略源对象null值
                    .setIgnoreError(true));   // 忽略类型转换错误
            existing.setUpdateTime(new Date());
            update(existing, queryWrapper); // 使用相同条件更新
        } else {
            // 4. 插入新记录
            CityGrid cityGrid = cityGridMapper.selectOne(
                    new LambdaQueryWrapper<CityGrid>()
                            .eq(CityGrid::getGridCode, gridCode));

            if (cityGrid == null) {
                log.debug("网格编码不存在: {}", gridCode);
                return;
            }

            BaseInfo newRecord = new BaseInfo();
            BeanUtil.copyProperties(source, newRecord);
            newRecord.setSourceArea(sourceArea);
            newRecord.setAreaName(district.getName());
            newRecord.setUpdateTime(new Date());
            newRecord.setStreetCode(cityGrid.getStreetCode());
            newRecord.setStreetName(cityGrid.getStreetName());
            newRecord.setWgAreaCode(cityGrid.getAreaCode());
            newRecord.setWgAreaName(cityGrid.getDistrictName());

            save(newRecord);
        }
    }


    /**
     * 汇集基础数据至市党建系统表
     */
    @Override
    public void baseToTarget() {
        LambdaQueryWrapper<BaseInfo> queryWrapper = new LambdaQueryWrapper<>();
        // 获取上次同步的最大更新时间
        Date lastSyncTime = sqlHelper.getLastSyncTimeFromTarget();
        if (lastSyncTime != null) {
            queryWrapper.gt(BaseInfo::getUpdateTime, lastSyncTime);
        }

        // 分页查询源库
        int pageSize = 1000;
        int current = 1;
        int totalMigrated = 0;
        boolean hasMoreData = true;

        Connection targetConn = sqlHelper.getTargetConn();
        try {
            targetConn.setAutoCommit(false);
            String insertSql = "INSERT INTO dwd_dj_xxly_info (" +
                    "grid_code, grid_name, sczt, cyry, zfll, jldzz, qzdw, qzdzz, qzdzb, " +
                    "fgqy, syfgqy, hhsyzqy, gtgsh, sqjjhmf, shzjzz, mbfqydw, lssws, kjssws, " +
                    "swssws, zcpgjg, jymbfqydw, ylwsjgmbfqydw, cpjrjg, dfjrjg, jq, ly, yq, " +
                    "sqsc, cun, dwfg, ddzjdzz, lhzjdzz, qyfg, lydzz, yqdzz, jqdzz, sqscdzz, " +
                    "cdzz, hyfg, wfg, nmsczts, tyssczts, process_state, source_area, area_name, " +
                    "street_code, street_name, jhpt_update_time, dsjzx_taskid" +
                    ") VALUES (" +
                    "?, ?, ?, ?, ?, ?, ?, ?, ?, " +  // 1-9
                    "?, ?, ?, ?, ?, ?, ?, ?, ?, " +  // 10-18
                    "?, ?, ?, ?, ?, ?, ?, ?, ?, " +  // 19-27
                    "?, ?, ?, ?, ?, ?, ?, ?, ?, " +  // 28-36
                    "?, ?, ?, ?, ?, ?, ?, ?, ?, " +  // 37-45
                    "?, ?, ?, ?)";             // 46-49

            String updateSql = "UPDATE dwd_dj_xxly_info SET " +
                    "grid_name = ?, sczt = ?, cyry = ?, zfll = ?, jldzz = ?, qzdw = ?, qzdzz = ?, qzdzb = ?, " +  // 2-9
                    "fgqy = ?, syfgqy = ?, hhsyzqy = ?, gtgsh = ?, sqjjhmf = ?, shzjzz = ?, mbfqydw = ?, lssws = ?, kjssws = ?, " +  // 10-18
                    "swssws = ?, zcpgjg = ?, jymbfqydw = ?, ylwsjgmbfqydw = ?, cpjrjg = ?, dfjrjg = ?, jq = ?, ly = ?, yq = ?, " +  // 19-27
                    "sqsc = ?, cun = ?, dwfg = ?, ddzjdzz = ?, lhzjdzz = ?, qyfg = ?, lydzz = ?, yqdzz = ?, jqdzz = ?, " +  // 28-36
                    "sqscdzz = ?, cdzz = ?, hyfg = ?, wfg = ?, nmsczts = ?, tyssczts = ?, process_state = ?, area_name = ?, " +  // 37-44
                    "street_code = ?, street_name = ?, jhpt_update_time = ?, dsjzx_taskid = ? " +  // 45-48
                    "WHERE source_area = ? AND grid_code = ?";  // 49-50（条件）

            while (hasMoreData) {
                Page<BaseInfo> page = new Page<>(current, pageSize);
                IPage<BaseInfo> pageResult = baseMapper.selectPage(page, queryWrapper);
                List<BaseInfo> records = pageResult.getRecords();

                List<BaseInfo> insertList = new ArrayList<>();
                List<BaseInfo> updateList = new ArrayList<>();

                // 终止条件：当前页无数据或已到达最后一页
                if (records.isEmpty() || records.size() < pageSize) {
                    hasMoreData = false;
                }

                if (!records.isEmpty()) {
                    for (BaseInfo record : records) {
                        boolean exists = checkIfRecordExists(targetConn, record.getSourceArea(), record.getGridCode());
                        if (exists) {
                            updateList.add(record);
                        } else {
                            insertList.add(record);
                        }
                    }
                    try (PreparedStatement pstmt = targetConn.prepareStatement(insertSql)) {
                        for (BaseInfo info : insertList) {
                            // 设置参数
                            int paramIndex = 1;

                            pstmt.setString(paramIndex++, info.getGridCode());
                            pstmt.setString(paramIndex++, info.getGridName());

                            // 所有整型字段（可能为null）
                            setNullableInt(pstmt, paramIndex++, info.getSczt());
                            setNullableInt(pstmt, paramIndex++, info.getCyry());
                            setNullableInt(pstmt, paramIndex++, info.getZfll());
                            setNullableInt(pstmt, paramIndex++, info.getJldzz());
                            setNullableInt(pstmt, paramIndex++, info.getQzdw());
                            setNullableInt(pstmt, paramIndex++, info.getQzdzz());
                            setNullableInt(pstmt, paramIndex++, info.getQzdzb());
                            setNullableInt(pstmt, paramIndex++, info.getFgqy());
                            setNullableInt(pstmt, paramIndex++, info.getSyfgqy());
                            setNullableInt(pstmt, paramIndex++, info.getHhsyzqy());
                            setNullableInt(pstmt, paramIndex++, info.getGtgsh());
                            setNullableInt(pstmt, paramIndex++, info.getSqjjhmf());
                            setNullableInt(pstmt, paramIndex++, info.getShzjzz());
                            setNullableInt(pstmt, paramIndex++, info.getMbfqydw());
                            setNullableInt(pstmt, paramIndex++, info.getLssws());
                            setNullableInt(pstmt, paramIndex++, info.getKjssws());
                            setNullableInt(pstmt, paramIndex++, info.getSwssws());
                            setNullableInt(pstmt, paramIndex++, info.getZcpgjg());
                            setNullableInt(pstmt, paramIndex++, info.getJymbfqydw());
                            setNullableInt(pstmt, paramIndex++, info.getYlwsjgmbfqydw());
                            setNullableInt(pstmt, paramIndex++, info.getCpjrjg());
                            setNullableInt(pstmt, paramIndex++, info.getDfjrjg());
                            setNullableInt(pstmt, paramIndex++, info.getJq());
                            setNullableInt(pstmt, paramIndex++, info.getLy());
                            setNullableInt(pstmt, paramIndex++, info.getYq());
                            setNullableInt(pstmt, paramIndex++, info.getSqsc());
                            setNullableInt(pstmt, paramIndex++, info.getCun());
                            setNullableInt(pstmt, paramIndex++, info.getDwfg());
                            setNullableInt(pstmt, paramIndex++, info.getDdzjdzz());
                            setNullableInt(pstmt, paramIndex++, info.getLhzjdzz());
                            setNullableInt(pstmt, paramIndex++, info.getQyfg());
                            setNullableInt(pstmt, paramIndex++, info.getLydzz());
                            setNullableInt(pstmt, paramIndex++, info.getYqdzz());
                            setNullableInt(pstmt, paramIndex++, info.getJqdzz());
                            setNullableInt(pstmt, paramIndex++, info.getSqscdzz());
                            setNullableInt(pstmt, paramIndex++, info.getCdzz());
                            setNullableInt(pstmt, paramIndex++, info.getHyfg());
                            setNullableInt(pstmt, paramIndex++, info.getWfg());
                            setNullableInt(pstmt, paramIndex++, info.getNmsczts());
                            setNullableInt(pstmt, paramIndex++, info.getTyssczts());

                            pstmt.setString(paramIndex++, info.getProcessState());
                            pstmt.setString(paramIndex++, info.getSourceArea());
                            pstmt.setString(paramIndex++, info.getAreaName());
                            pstmt.setString(paramIndex++, info.getStreetCode());
                            pstmt.setString(paramIndex++, info.getStreetName());
                            pstmt.setTimestamp(paramIndex++, new Timestamp(info.getUpdateTime().getTime()));
                            pstmt.setString(paramIndex++, info.getUpdateTime().toInstant()
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
                            pstmt.addBatch();
                        }

                        pstmt.executeBatch();
                        targetConn.commit();
                        totalMigrated += insertList.size();
                        log.info("已推送{}条数据，当前进度: {}/{}",
                                insertList.size(), totalMigrated, pageResult.getRecords().size());
                    }

                    try (PreparedStatement pstmt = targetConn.prepareStatement(updateSql)) {
                        for (BaseInfo info : records) {
                            int paramIndex = 1; // 从第1个参数开始设置

                            // SET 部分（按字段顺序）
                            pstmt.setString(paramIndex++, info.getGridName());      // grid_name
                            setNullableInt(pstmt, paramIndex++, info.getSczt());    // sczt
                            setNullableInt(pstmt, paramIndex++, info.getCyry());    // cyry
                            setNullableInt(pstmt, paramIndex++, info.getZfll());    // zfll
                            setNullableInt(pstmt, paramIndex++, info.getJldzz());   // jldzz
                            setNullableInt(pstmt, paramIndex++, info.getQzdw());    // qzdw
                            setNullableInt(pstmt, paramIndex++, info.getQzdzz());   // qzdzz
                            setNullableInt(pstmt, paramIndex++, info.getQzdzb());   // qzdzb
                            setNullableInt(pstmt, paramIndex++, info.getFgqy());   // fgqy
                            setNullableInt(pstmt, paramIndex++, info.getSyfgqy()); // syfgqy
                            setNullableInt(pstmt, paramIndex++, info.getHhsyzqy()); // hhsyzqy
                            setNullableInt(pstmt, paramIndex++, info.getGtgsh());   // gtgsh
                            setNullableInt(pstmt, paramIndex++, info.getSqjjhmf()); // sqjjhmf
                            setNullableInt(pstmt, paramIndex++, info.getShzjzz());  // shzjzz
                            setNullableInt(pstmt, paramIndex++, info.getMbfqydw()); // mbfqydw
                            setNullableInt(pstmt, paramIndex++, info.getLssws());   // lssws
                            setNullableInt(pstmt, paramIndex++, info.getKjssws());  // kjssws
                            setNullableInt(pstmt, paramIndex++, info.getSwssws());   // swssws
                            setNullableInt(pstmt, paramIndex++, info.getZcpgjg());   // zcpgjg
                            setNullableInt(pstmt, paramIndex++, info.getJymbfqydw());// jymbfqydw
                            setNullableInt(pstmt, paramIndex++, info.getYlwsjgmbfqydw()); // ylwsjgmbfqydw
                            setNullableInt(pstmt, paramIndex++, info.getCpjrjg());   // cpjrjg
                            setNullableInt(pstmt, paramIndex++, info.getDfjrjg());   // dfjrjg
                            setNullableInt(pstmt, paramIndex++, info.getJq());       // jq
                            setNullableInt(pstmt, paramIndex++, info.getLy());       // ly
                            setNullableInt(pstmt, paramIndex++, info.getYq());       // yq
                            setNullableInt(pstmt, paramIndex++, info.getSqsc());     // sqsc
                            setNullableInt(pstmt, paramIndex++, info.getCun());      // cun
                            setNullableInt(pstmt, paramIndex++, info.getDwfg());     // dwfg
                            setNullableInt(pstmt, paramIndex++, info.getDdzjdzz());  // ddzjdzz
                            setNullableInt(pstmt, paramIndex++, info.getLhzjdzz());  // lhzjdzz
                            setNullableInt(pstmt, paramIndex++, info.getQyfg());     // qyfg
                            setNullableInt(pstmt, paramIndex++, info.getLydzz());    // lydzz
                            setNullableInt(pstmt, paramIndex++, info.getYqdzz());    // yqdzz
                            setNullableInt(pstmt, paramIndex++, info.getJqdzz());    // jqdzz
                            setNullableInt(pstmt, paramIndex++, info.getSqscdzz());  // sqscdzz
                            setNullableInt(pstmt, paramIndex++, info.getCdzz());     // cdzz
                            setNullableInt(pstmt, paramIndex++, info.getHyfg());     // hyfg
                            setNullableInt(pstmt, paramIndex++, info.getWfg());      // wfg
                            setNullableInt(pstmt, paramIndex++, info.getNmsczts());   // nmsczts
                            setNullableInt(pstmt, paramIndex++, info.getTyssczts());  // tyssczts
                            pstmt.setString(paramIndex++, info.getProcessState());    // process_state
                            pstmt.setString(paramIndex++, info.getAreaName());        // area_name
                            pstmt.setString(paramIndex++, info.getStreetCode());      // street_code
                            pstmt.setString(paramIndex++, info.getStreetName());      // street_name
                            pstmt.setTimestamp(paramIndex++, new Timestamp(info.getUpdateTime().getTime())); // jhpt_update_time
                            pstmt.setString(paramIndex++, info.getUpdateTime().toInstant()
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate().format(DateTimeFormatter.ofPattern("yyyyMMdd"))); // dsjzx_taskid

                            // WHERE 条件（最后两个参数）
                            pstmt.setString(paramIndex++, info.getSourceArea());      // source_area
                            pstmt.setString(paramIndex++, info.getGridCode());        // grid_code

                            pstmt.addBatch(); // 添加到批处理
                        }
                        pstmt.executeBatch(); // 执行批量更新
                        targetConn.commit();  // 提交事务
                        totalMigrated += updateList.size();
                        log.info("已更新{}条数据，当前进度: {}/{}",
                                updateList.size(), totalMigrated, pageResult.getRecords().size());
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
    public void baseToDmTarget() {
        LambdaQueryWrapper<BaseInfo> queryWrapper = new LambdaQueryWrapper<>();
        // 获取上次同步的最大更新时间
        Date lastSyncTime = sqlHelper.getLastSyncTimeFromDmTarget();
        if (lastSyncTime != null) {
            queryWrapper.gt(BaseInfo::getUpdateTime, lastSyncTime);
        }

        // 分页查询源库
        int pageSize = 1000;
        int current = 1;
        int totalMigrated = 0;
        boolean hasMoreData = true;

        Connection targetConn = sqlHelper.getDmTargetConn();
        try {
            targetConn.setAutoCommit(false);
            String insertSql = "INSERT INTO dwd_dj_xxly_info (" +
                    "grid_code, grid_name, sczt, cyry, zfll, jldzz, qzdw, qzdzz, qzdzb, " +
                    "fgqy, syfgqy, hhsyzqy, gtgsh, sqjjhmf, shzjzz, mbfqydw, lssws, kjssws, " +
                    "swssws, zcpgjg, jymbfqydw, ylwsjgmbfqydw, cpjrjg, dfjrjg, jq, ly, yq, " +
                    "sqsc, cun, dwfg, ddzjdzz, lhzjdzz, qyfg, lydzz, yqdzz, jqdzz, sqscdzz, " +
                    "cdzz, hyfg, wfg, nmsczts, tyssczts, process_state, source_area, area_name, " +
                    "street_code, street_name, jhpt_update_time, dsjzx_taskid" +
                    ") VALUES (" +
                    "?, ?, ?, ?, ?, ?, ?, ?, ?, " +  // 1-9
                    "?, ?, ?, ?, ?, ?, ?, ?, ?, " +  // 10-18
                    "?, ?, ?, ?, ?, ?, ?, ?, ?, " +  // 19-27
                    "?, ?, ?, ?, ?, ?, ?, ?, ?, " +  // 28-36
                    "?, ?, ?, ?, ?, ?, ?, ?, ?, " +  // 37-45
                    "?, ?, ?, ?)";             // 46-49

            String updateSql = "UPDATE dwd_dj_xxly_info SET " +
                    "grid_name = ?, sczt = ?, cyry = ?, zfll = ?, jldzz = ?, qzdw = ?, qzdzz = ?, qzdzb = ?, " +  // 2-9
                    "fgqy = ?, syfgqy = ?, hhsyzqy = ?, gtgsh = ?, sqjjhmf = ?, shzjzz = ?, mbfqydw = ?, lssws = ?, kjssws = ?, " +  // 10-18
                    "swssws = ?, zcpgjg = ?, jymbfqydw = ?, ylwsjgmbfqydw = ?, cpjrjg = ?, dfjrjg = ?, jq = ?, ly = ?, yq = ?, " +  // 19-27
                    "sqsc = ?, cun = ?, dwfg = ?, ddzjdzz = ?, lhzjdzz = ?, qyfg = ?, lydzz = ?, yqdzz = ?, jqdzz = ?, " +  // 28-36
                    "sqscdzz = ?, cdzz = ?, hyfg = ?, wfg = ?, nmsczts = ?, tyssczts = ?, process_state = ?, area_name = ?, " +  // 37-44
                    "street_code = ?, street_name = ?, jhpt_update_time = ?, dsjzx_taskid = ? " +  // 45-48
                    "WHERE source_area = ? AND grid_code = ?";  // 49-50（条件）

            while (hasMoreData) {
                Page<BaseInfo> page = new Page<>(current, pageSize);
                IPage<BaseInfo> pageResult = baseMapper.selectPage(page, queryWrapper);
                List<BaseInfo> records = pageResult.getRecords();

                List<BaseInfo> insertList = new ArrayList<>();
                List<BaseInfo> updateList = new ArrayList<>();

                // 终止条件：当前页无数据或已到达最后一页
                if (records.isEmpty() || records.size() < pageSize) {
                    hasMoreData = false;
                }

                if (!records.isEmpty()) {
                    for (BaseInfo record : records) {
                        boolean exists = checkIfRecordExists(targetConn, record.getSourceArea(), record.getGridCode());
                        if (exists) {
                            updateList.add(record);
                        } else {
                            insertList.add(record);
                        }
                    }
                    try (PreparedStatement pstmt = targetConn.prepareStatement(insertSql)) {
                        for (BaseInfo info : insertList) {
                            // 设置参数
                            int paramIndex = 1;

                            pstmt.setString(paramIndex++, info.getGridCode());
                            pstmt.setString(paramIndex++, info.getGridName());

                            // 所有整型字段（可能为null）
                            setNullableInt(pstmt, paramIndex++, info.getSczt());
                            setNullableInt(pstmt, paramIndex++, info.getCyry());
                            setNullableInt(pstmt, paramIndex++, info.getZfll());
                            setNullableInt(pstmt, paramIndex++, info.getJldzz());
                            setNullableInt(pstmt, paramIndex++, info.getQzdw());
                            setNullableInt(pstmt, paramIndex++, info.getQzdzz());
                            setNullableInt(pstmt, paramIndex++, info.getQzdzb());
                            setNullableInt(pstmt, paramIndex++, info.getFgqy());
                            setNullableInt(pstmt, paramIndex++, info.getSyfgqy());
                            setNullableInt(pstmt, paramIndex++, info.getHhsyzqy());
                            setNullableInt(pstmt, paramIndex++, info.getGtgsh());
                            setNullableInt(pstmt, paramIndex++, info.getSqjjhmf());
                            setNullableInt(pstmt, paramIndex++, info.getShzjzz());
                            setNullableInt(pstmt, paramIndex++, info.getMbfqydw());
                            setNullableInt(pstmt, paramIndex++, info.getLssws());
                            setNullableInt(pstmt, paramIndex++, info.getKjssws());
                            setNullableInt(pstmt, paramIndex++, info.getSwssws());
                            setNullableInt(pstmt, paramIndex++, info.getZcpgjg());
                            setNullableInt(pstmt, paramIndex++, info.getJymbfqydw());
                            setNullableInt(pstmt, paramIndex++, info.getYlwsjgmbfqydw());
                            setNullableInt(pstmt, paramIndex++, info.getCpjrjg());
                            setNullableInt(pstmt, paramIndex++, info.getDfjrjg());
                            setNullableInt(pstmt, paramIndex++, info.getJq());
                            setNullableInt(pstmt, paramIndex++, info.getLy());
                            setNullableInt(pstmt, paramIndex++, info.getYq());
                            setNullableInt(pstmt, paramIndex++, info.getSqsc());
                            setNullableInt(pstmt, paramIndex++, info.getCun());
                            setNullableInt(pstmt, paramIndex++, info.getDwfg());
                            setNullableInt(pstmt, paramIndex++, info.getDdzjdzz());
                            setNullableInt(pstmt, paramIndex++, info.getLhzjdzz());
                            setNullableInt(pstmt, paramIndex++, info.getQyfg());
                            setNullableInt(pstmt, paramIndex++, info.getLydzz());
                            setNullableInt(pstmt, paramIndex++, info.getYqdzz());
                            setNullableInt(pstmt, paramIndex++, info.getJqdzz());
                            setNullableInt(pstmt, paramIndex++, info.getSqscdzz());
                            setNullableInt(pstmt, paramIndex++, info.getCdzz());
                            setNullableInt(pstmt, paramIndex++, info.getHyfg());
                            setNullableInt(pstmt, paramIndex++, info.getWfg());
                            setNullableInt(pstmt, paramIndex++, info.getNmsczts());
                            setNullableInt(pstmt, paramIndex++, info.getTyssczts());

                            pstmt.setString(paramIndex++, "0");
                            pstmt.setString(paramIndex++, info.getSourceArea());
                            pstmt.setString(paramIndex++, info.getAreaName());
                            pstmt.setString(paramIndex++, info.getStreetCode());
                            pstmt.setString(paramIndex++, info.getStreetName());
                            pstmt.setTimestamp(paramIndex++, new Timestamp(info.getUpdateTime().getTime()));
                            pstmt.setString(paramIndex++, info.getUpdateTime().toInstant()
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
                            pstmt.addBatch();
                        }

                        pstmt.executeBatch();
                        targetConn.commit();
                        totalMigrated += insertList.size();
                        log.info("已推送{}条数据，当前进度: {}/{}",
                                insertList.size(), totalMigrated, pageResult.getRecords().size());
                    }

                    try (PreparedStatement pstmt = targetConn.prepareStatement(updateSql)) {
                        for (BaseInfo info : records) {
                            int paramIndex = 1; // 从第1个参数开始设置

                            // SET 部分（按字段顺序）
                            pstmt.setString(paramIndex++, info.getGridName());      // grid_name
                            setNullableInt(pstmt, paramIndex++, info.getSczt());    // sczt
                            setNullableInt(pstmt, paramIndex++, info.getCyry());    // cyry
                            setNullableInt(pstmt, paramIndex++, info.getZfll());    // zfll
                            setNullableInt(pstmt, paramIndex++, info.getJldzz());   // jldzz
                            setNullableInt(pstmt, paramIndex++, info.getQzdw());    // qzdw
                            setNullableInt(pstmt, paramIndex++, info.getQzdzz());   // qzdzz
                            setNullableInt(pstmt, paramIndex++, info.getQzdzb());   // qzdzb
                            setNullableInt(pstmt, paramIndex++, info.getFgqy());   // fgqy
                            setNullableInt(pstmt, paramIndex++, info.getSyfgqy()); // syfgqy
                            setNullableInt(pstmt, paramIndex++, info.getHhsyzqy()); // hhsyzqy
                            setNullableInt(pstmt, paramIndex++, info.getGtgsh());   // gtgsh
                            setNullableInt(pstmt, paramIndex++, info.getSqjjhmf()); // sqjjhmf
                            setNullableInt(pstmt, paramIndex++, info.getShzjzz());  // shzjzz
                            setNullableInt(pstmt, paramIndex++, info.getMbfqydw()); // mbfqydw
                            setNullableInt(pstmt, paramIndex++, info.getLssws());   // lssws
                            setNullableInt(pstmt, paramIndex++, info.getKjssws());  // kjssws
                            setNullableInt(pstmt, paramIndex++, info.getSwssws());   // swssws
                            setNullableInt(pstmt, paramIndex++, info.getZcpgjg());   // zcpgjg
                            setNullableInt(pstmt, paramIndex++, info.getJymbfqydw());// jymbfqydw
                            setNullableInt(pstmt, paramIndex++, info.getYlwsjgmbfqydw()); // ylwsjgmbfqydw
                            setNullableInt(pstmt, paramIndex++, info.getCpjrjg());   // cpjrjg
                            setNullableInt(pstmt, paramIndex++, info.getDfjrjg());   // dfjrjg
                            setNullableInt(pstmt, paramIndex++, info.getJq());       // jq
                            setNullableInt(pstmt, paramIndex++, info.getLy());       // ly
                            setNullableInt(pstmt, paramIndex++, info.getYq());       // yq
                            setNullableInt(pstmt, paramIndex++, info.getSqsc());     // sqsc
                            setNullableInt(pstmt, paramIndex++, info.getCun());      // cun
                            setNullableInt(pstmt, paramIndex++, info.getDwfg());     // dwfg
                            setNullableInt(pstmt, paramIndex++, info.getDdzjdzz());  // ddzjdzz
                            setNullableInt(pstmt, paramIndex++, info.getLhzjdzz());  // lhzjdzz
                            setNullableInt(pstmt, paramIndex++, info.getQyfg());     // qyfg
                            setNullableInt(pstmt, paramIndex++, info.getLydzz());    // lydzz
                            setNullableInt(pstmt, paramIndex++, info.getYqdzz());    // yqdzz
                            setNullableInt(pstmt, paramIndex++, info.getJqdzz());    // jqdzz
                            setNullableInt(pstmt, paramIndex++, info.getSqscdzz());  // sqscdzz
                            setNullableInt(pstmt, paramIndex++, info.getCdzz());     // cdzz
                            setNullableInt(pstmt, paramIndex++, info.getHyfg());     // hyfg
                            setNullableInt(pstmt, paramIndex++, info.getWfg());      // wfg
                            setNullableInt(pstmt, paramIndex++, info.getNmsczts());   // nmsczts
                            setNullableInt(pstmt, paramIndex++, info.getTyssczts());  // tyssczts
                            pstmt.setString(paramIndex++, "0");    // process_state
                            pstmt.setString(paramIndex++, info.getAreaName());        // area_name
                            pstmt.setString(paramIndex++, info.getStreetCode());      // street_code
                            pstmt.setString(paramIndex++, info.getStreetName());      // street_name
                            pstmt.setTimestamp(paramIndex++, new Timestamp(info.getUpdateTime().getTime())); // jhpt_update_time
                            pstmt.setString(paramIndex++, info.getUpdateTime().toInstant()
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate().format(DateTimeFormatter.ofPattern("yyyyMMdd"))); // dsjzx_taskid

                            // WHERE 条件（最后两个参数）
                            pstmt.setString(paramIndex++, info.getSourceArea());      // source_area
                            pstmt.setString(paramIndex++, info.getGridCode());        // grid_code

                            pstmt.addBatch(); // 添加到批处理
                        }
                        pstmt.executeBatch(); // 执行批量更新
                        targetConn.commit();  // 提交事务
                        totalMigrated += updateList.size();
                        log.info("已更新{}条数据，当前进度: {}/{}",
                                updateList.size(), totalMigrated, pageResult.getRecords().size());
                    }
                }
                current++;
            }
        } catch (SQLException e) {
            throw new RuntimeException("跨库数据库数据推送失败", e);
        }

        log.info("数据推送完成，共推送了{}条数据", totalMigrated);
    }


    private void setNullableInt(PreparedStatement pstmt, int paramIndex, Integer value) throws SQLException {
        if (value != null) {
            pstmt.setInt(paramIndex, value);
        } else {
            pstmt.setNull(paramIndex, Types.INTEGER);
        }
    }

    private boolean checkIfRecordExists(Connection conn, String sourceArea, String gridCode) throws SQLException {
        String sql = "SELECT 1 FROM dwd_dj_xxly_info WHERE source_area = ? AND grid_code = ? LIMIT 1";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, sourceArea);
            pstmt.setString(2, gridCode);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next(); // 如果有数据返回 true，否则 false
            }
        }
    }


}
