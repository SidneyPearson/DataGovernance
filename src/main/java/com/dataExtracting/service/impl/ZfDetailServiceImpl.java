package com.dataExtracting.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dataExtracting.domain.entity.*;
import com.dataExtracting.domain.enums.DistrictEnum;
import com.dataExtracting.helper.SqlHelper;
import com.dataExtracting.mapper.CityGridMapper;
import com.dataExtracting.mapper.ZfDetailMapper;
import com.dataExtracting.service.ZfDetailService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Date;
import java.util.stream.Collectors;


@Service
public class ZfDetailServiceImpl extends ServiceImpl<ZfDetailMapper, ZfDetail>
        implements ZfDetailService {

    @Autowired
    private SqlHelper sqlHelper;
    @Autowired
    private CityGridMapper cityGridMapper;

    private final String TODAY_STR = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    private static final Logger log = LoggerFactory.getLogger(ZfDetailServiceImpl.class);
    private static final int BATCH_SIZE = 5000; // 每批处理5000条数据
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sourceToBase(District district, boolean isFirst) {
        DistrictEnum districtEnum = DistrictEnum.fromName(district.getName());

        // 1. 获取源数据
        List<SourceDetailObj> sourceDetailObjs = sqlHelper.getSourceDetailInfoData(
                districtEnum.getUsername(),
                districtEnum.getPassword(),
                districtEnum.getSchema(),
                isFirst ? null : TODAY_STR
        );
        log.info("获取源数据数量: 【{}】", sourceDetailObjs.size());

        // 2. 过滤有效数据（必须包含网格编码）
        List<SourceDetailObj> validSources = sourceDetailObjs.stream()
                .filter(s -> StringUtils.isNotEmpty(s.getGridCode())
                        && StringUtils.isNotEmpty(s.getVisitTime()))
                .collect(Collectors.collectingAndThen(
                        Collectors.groupingBy(
                                s -> s.getShopCode() + "|" + s.getVisitTime(), // 组合成唯一键
                                Collectors.toList()
                        ),
                        map -> map.values().stream()
                                .map(list -> list.get(new Random().nextInt(list.size()))) // 随机取一条
                                .collect(Collectors.toList())
                ));

        // 3. 批量转换实体
        List<ZfDetail> entities = validSources.stream()
                .map(s -> convertToEntity(s, district))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        log.info("开始应用层唯一性校验！");
        // 4. 应用层唯一性校验
        if (!entities.isEmpty()) {
            // 根据shop_code,visit_time和source_area查看是否存在
            Set<String> existingKeys = getExistingUniqueKeys(entities);

            // 过滤掉已存在的数据
            List<ZfDetail> newEntities = entities.stream()
                    .filter(e -> !existingKeys.contains(generateUniqueKey(e)))
                    .collect(Collectors.toList());

            // 5. 批量插入
            if (!newEntities.isEmpty()) {
                log.info("开始插入数据！");
                saveBatch(newEntities);
                log.info("成功插入{}条数据，跳过{}条重复数据",
                        newEntities.size(), entities.size() - newEntities.size());
            } else {
                log.info("所有数据均已存在，无需插入");
            }
        }
    }

    private String generateUniqueKey(ZfDetail entity) {
        return entity.getShopCode() + "|" + entity.getVisitTime().getTime() + "|" + entity.getSourceArea();
    }

    // 查询已存在的唯一键组合
    private Set<String> getExistingUniqueKeys(List<ZfDetail> entities) {
        Set<String> existingKeys = new HashSet<>();
        int batchSize = 1000; // 每批处理1000条

        // 按批次处理
        for (int i = 0; i < entities.size(); i += batchSize) {
            List<ZfDetail> batch = entities.subList(i, Math.min(i + batchSize, entities.size()));

            // 构建临时表查询条件
            List<Map<String, Object>> batchConditions = batch.stream()
                    .map(entity -> {
                        Map<String, Object> condition = new HashMap<>();
                        condition.put("shopCode", entity.getShopCode());
                        condition.put("visitTime", entity.getVisitTime());
                        condition.put("sourceArea", entity.getSourceArea());
                        return condition;
                    })
                    .collect(Collectors.toList());

            // 分批查询
            // 1. 直接使用 or 条件，去掉 nested
            LambdaQueryWrapper<ZfDetail> wrapper = new LambdaQueryWrapper<>();
            batchConditions.forEach(condition ->
                    wrapper.or(w -> w
                            .eq(ZfDetail::getShopCode, condition.get("shopCode"))
                            .eq(ZfDetail::getVisitTime, condition.get("visitTime"))
                            .eq(ZfDetail::getSourceArea, condition.get("sourceArea"))
                    )
            );

            log.info("batchConditions=========>{}", batchConditions.size());

            // 3. 执行查询
            List<ZfDetail> existing = baseMapper.selectList(wrapper);

            existingKeys.addAll(existing.stream()
                    .map(this::generateUniqueKey)
                    .collect(Collectors.toSet()));

        }

        return existingKeys;
    }

    private ZfDetail convertToEntity(SourceDetailObj source, District district) {
        // 基础属性
        ZfDetail entity = new ZfDetail();
        BeanUtil.copyProperties(source, entity, true);
        entity.setSourceArea(district.getCode());
        entity.setAreaName(district.getName());
        entity.setUpdateTime(new Date());

        // 查询网格信息
        CityGrid grid = cityGridMapper.selectOne(
                new LambdaQueryWrapper<CityGrid>()
                        .eq(CityGrid::getAreaCode, district.getCode())
                        .eq(CityGrid::getGridCode, source.getGridCode()));

        if (grid == null) {
            log.info("网格编码不存在或该区不存在这个网格编码: {}", source.getGridCode());
            return null;
        }

        // 设置网格相关字段
        entity.setStreetCode(grid.getStreetCode());
        entity.setStreetName(grid.getStreetName());
        entity.setWgAreaCode(grid.getAreaCode());
        entity.setWgAreaName(grid.getDistrictName());

        return entity;
    }

    /**
     * 汇集走访数据至市党建系统表
     */
    @Override
    public void baseToTarget() {
        LambdaQueryWrapper<ZfDetail> queryWrapper = new LambdaQueryWrapper<>();
        // 获取上次同步的最大更新时间
        Date lastSyncTime = sqlHelper.getLastSyncTimeFromTargetDetail();
        log.info("上次同步的最大更新时间===>{}", lastSyncTime);
        if (lastSyncTime != null) {
            queryWrapper.gt(ZfDetail::getUpdateTime, lastSyncTime);
        }

        // 分页查询源库
        int pageSize = 1000;
        int current = 1;
        int totalMigrated = 0;
        long totalRecords = baseMapper.selectCount(queryWrapper); // 先获取总记录数

        if (totalRecords == 0) {
            log.info("没有需要迁移的数据");
            return;
        }

        Connection targetConn = sqlHelper.getTargetConn();
        try {
            targetConn.setAutoCommit(false);
            String insertSql = "INSERT INTO dwd_dj_xxly_zf_detail (" +
                    "grid_code, grid_name, shop_name, shop_code, visit_time, visit_num, " +
                    "process_state, source_area, area_name, " +
                    "street_code, street_name, jhpt_update_time, dsjzx_taskid" +
                    ") VALUES (" +
                    "?, ?, ?, ?, ?, ?, ?, ?, ?, " +  // 1-9
                    "?, ?, ?, ?)";       // 10-13

            while (totalMigrated < totalRecords) {  // 明确的终止条件
                Page<ZfDetail> page = new Page<>(current, pageSize);
                IPage<ZfDetail> pageResult = baseMapper.selectPage(page, queryWrapper);
                List<ZfDetail> records = pageResult.getRecords();

                if (records.isEmpty()) {
                    log.warn("查询返回空记录但总数不为0，可能数据被并发修改，强制终止");
                    break;
                }

                try (PreparedStatement pstmt = targetConn.prepareStatement(insertSql)) {
                    for (ZfDetail info : records) {
                        // 设置参数
                        int paramIndex = 1;

                        pstmt.setString(paramIndex++, info.getGridCode());
                        pstmt.setString(paramIndex++, info.getGridName());
                        pstmt.setString(paramIndex++, info.getShopName());
                        pstmt.setString(paramIndex++, info.getShopCode());
                        pstmt.setTimestamp(paramIndex++, info.getVisitTime() == null ?
                                null : new Timestamp(info.getVisitTime().getTime()));

                        // 所有整型字段（可能为null）
                        setNullableInt(pstmt, paramIndex++, info.getVisitNum());

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
                    totalMigrated += records.size();
                    log.info("已迁移{}条数据，当前进度: {}/{}",
                            records.size(), totalMigrated, totalRecords);

                    // 安全检查：防止意外无限循环
                    if (current > (totalRecords / pageSize + 2)) {
                        log.error("分页次数异常，强制终止循环");
                        break;
                    }
                }
                current++;
            }
        } catch (SQLException e) {
            try {
                targetConn.rollback();
            } catch (SQLException ex) {
                log.error("回滚失败", ex);
            }
            throw new RuntimeException("跨库数据库数据推送失败", e);
        } finally {
            try {
                targetConn.setAutoCommit(true);
                targetConn.close();
            } catch (SQLException e) {
                log.error("关闭连接失败", e);
            }
        }

        log.info("数据推送完成，共推送了{}条数据", totalMigrated);
    }

    /**
     * 汇集走访数据至达梦目标表（优化版）
     */
    @Override
    public void baseToDmTarget() {
        // 1. 获取上次同步时间
        Date lastSyncTime = sqlHelper.getLastSyncTimeFromDmTargetDetail();
        log.info("上次同步时间: {}", lastSyncTime);

        // 2. 构建查询条件
        LambdaQueryWrapper<ZfDetail> queryWrapper = new LambdaQueryWrapper<>();
        if (lastSyncTime != null) {
            queryWrapper.gt(ZfDetail::getUpdateTime, lastSyncTime);
        }
        queryWrapper.orderByAsc(ZfDetail::getUpdateTime); // 按时间排序避免遗漏

        // 3. 分页查询
        int totalMigrated = 0;
        long startTime = System.currentTimeMillis();

        try (Connection targetConn = sqlHelper.getDmTargetConn()) {
            targetConn.setAutoCommit(false); // 关闭自动提交

            // 4. 预编译SQL（使用批量插入）
            String insertSql = buildInsertSql();
            try (PreparedStatement pstmt = targetConn.prepareStatement(insertSql)) {

                int pageNum = 1;
                while (true) {
                    // 5. 分页查询源数据
                    Page<ZfDetail> page = new Page<>(pageNum, BATCH_SIZE);
                    Page<ZfDetail> zfDetailPage = getBaseMapper().selectPage(page, queryWrapper);
                    List<ZfDetail> records = zfDetailPage.getRecords();
                    log.info("records: {}", records.size());
                    if (records.isEmpty()) {
                        log.info("数据分页 {} 无更多数据", pageNum);
                        break;
                    }

                    // 6. 批量处理当前页数据
                    int batchCount = 0;
                    for (ZfDetail data : records) {
                        setPreparedStatementParams(pstmt, data);
                        pstmt.addBatch();

                        if (++batchCount % BATCH_SIZE == 0) {
                            log.info("batchCount: {}", batchCount);
                            executeBatch(pstmt, targetConn);
                        }
                    }

                    // 7. 提交剩余批次
                    if (batchCount % BATCH_SIZE != 0) {
                        executeBatch(pstmt, targetConn);
                    }

                    totalMigrated += records.size();
                    logProgress(startTime, totalMigrated, records.size());

                    pageNum++; // 下一页
                }
            } catch (SQLException e) {
                targetConn.rollback();
                throw new RuntimeException("批量插入失败", e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("数据库连接异常", e);
        }

        log.info("同步完成，总计处理 {} 条数据，耗时 {} 秒",
                totalMigrated,
                (System.currentTimeMillis() - startTime) / 1000);
    }

    // ============== 私有方法 ==============
    private String buildInsertSql() {
        return "INSERT INTO dwd_dj_xxly_zf_detail (" +
                "grid_code, grid_name, shop_name, shop_code, " +
                "visit_time, visit_num, process_state, " +
                "source_area, area_name, street_code, " +
                "street_name, jhpt_update_time, dsjzx_taskid" +
                ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
    }

    private void setPreparedStatementParams(PreparedStatement pstmt, ZfDetail data)
            throws SQLException {

        int index = 1;
        pstmt.setString(index++, data.getGridCode());
        pstmt.setString(index++, data.getGridName());
        pstmt.setString(index++, data.getShopName());
        pstmt.setString(index++, data.getShopCode());
        pstmt.setTimestamp(index++, data.getVisitTime() != null ?
                new Timestamp(data.getVisitTime().getTime()) : null);
        setNullableInt(pstmt, index++, 1);
        pstmt.setString(index++, "0");
        pstmt.setString(index++, data.getSourceArea());
        pstmt.setString(index++, data.getAreaName());
        pstmt.setString(index++, data.getStreetCode());
        pstmt.setString(index++, data.getStreetName());
        pstmt.setTimestamp(index++, new Timestamp(data.getUpdateTime().getTime()));
        pstmt.setString(index++, data.getUpdateTime().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate().format(DATE_FORMATTER));
    }


    private void executeBatch(PreparedStatement pstmt, Connection conn)
            throws SQLException {

        int[] counts = pstmt.executeBatch();
        conn.commit();
        pstmt.clearBatch();
        log.info("已提交批次，影响行数: {}", counts.length);
    }

    private void logProgress(long startTime, int totalProcessed, int batchSize) {
        long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
        log.info("已处理: {} 条 | 当前批次: {} 条 | 耗时: {} 秒",
                totalProcessed, batchSize, elapsedSeconds);
    }


    private void setNullableInt(PreparedStatement pstmt, int paramIndex, Integer value) throws SQLException {
        if (value != null) {
            pstmt.setInt(paramIndex, value);
        } else {
            pstmt.setNull(paramIndex, Types.INTEGER);
        }
    }


}
