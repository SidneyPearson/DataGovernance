package com.dataExtracting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dataExtracting.domain.entity.*;
import com.dataExtracting.domain.enums.DistrictEnum;
import com.dataExtracting.helper.SqlHelper;
import com.dataExtracting.mapper.DzzDetailMapper;
import com.dataExtracting.service.DzzDetailService;
import com.dataExtracting.utils.DBConnectionUtils;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
public class DzzDetailServiceImpl extends ServiceImpl<DzzDetailMapper, DzzDetail>
        implements DzzDetailService {

    private static final Logger log = LoggerFactory.getLogger(DzzDetailServiceImpl.class);


    @Autowired
    private SqlHelper sqlHelper;

    private static final int BATCH_SIZE = 1000;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void getDzzData() {
        // 每次获取数据，先进行清表
        remove(new QueryWrapper<>());
        Connection targetConn = sqlHelper.getDmTargetConn();
        List<DzzDetail> dzzDetails = new ArrayList<>();
        try {
            targetConn.setAutoCommit(false);
            String querySql = "select * from dwd_dj_xxly_dzz_detail";
            Statement stmt = targetConn.createStatement();
            ResultSet rs = stmt.executeQuery(querySql);
            while (rs.next()) {
                DzzDetail detail = new DzzDetail();
                detail.setWgCode(rs.getString("wg_code"));
                detail.setWgName(rs.getString("wg_name"));
                detail.setOrgCode(rs.getString("org_code"));
                detail.setOrgName(rs.getString("org_name"));
                detail.setOrgType(rs.getString("org_type"));
                detail.setCommunityName(rs.getString("community_name"));
                detail.setEmergingFiledType(rs.getString("emerging_filed_type"));
                detail.setUnitType(rs.getString("unit_type"));
                detail.setDzzlxUpdateTime(rs.getTimestamp("dzzlx_update_time"));
                detail.setDzzlxTaskid(rs.getString("dzzlx_taskid"));
                detail.setUpdateTime(new Date());
                setAreaNameIfContainsDistrict(detail);
                dzzDetails.add(detail);
                if (dzzDetails.size() >= BATCH_SIZE) {
                    saveBatch(dzzDetails);
                    dzzDetails.clear();
                }
            }

            // Save any remaining records
            if (!dzzDetails.isEmpty()) {
                saveBatch(dzzDetails);
            }
        } catch (SQLException e) {
            log.error("Failed to fetch data from target database", e);
            throw new RuntimeException("跨库数据库数据推送查询失败", e);
        }
    }

    @Override
    public void pushDzzData() {
        for (DistrictEnum district : DistrictEnum.values()) {
            LambdaQueryWrapper<DzzDetail> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(DzzDetail::getAreaName, district.getName());
            List<DzzDetail> dzzDetails = list(queryWrapper);
            pushDzzDataByAreaName(dzzDetails);
        }
    }


    private void pushDzzDataByAreaName(List<DzzDetail> dzzDetails) {
        if (CollectionUtils.isEmpty(dzzDetails)) {
            log.info("该区无党组织数据需要推送");
            return;
        }

        try {
            // 获取推送所需的连接信息
            DistrictEnum districtEnum = DistrictEnum.fromName(dzzDetails.get(0).getAreaName());
            LocalDateTime now = LocalDateTime.now();
            String taskId = now.format(DateTimeFormatter.ofPattern("yyyyMMdd")); // 仅包含日期
            Timestamp currentTimestamp = Timestamp.valueOf(now);

            // 获取数据库连接
            try (Connection connection = DBConnectionUtils.getRDJCConnection(
                    districtEnum.getUsername(),
                    districtEnum.getPassword(),
                    districtEnum.getSchema())) {

                // 1. 先清空目标表中该区域的数据
                String clearSql = "TRUNCATE TABLE dwd_dj_xxly_dzz_detail";
                Statement stmt = connection.createStatement();
                stmt.executeUpdate(clearSql);

                // 2. 准备批量插入语句
                try (PreparedStatement pushStatement = connection.prepareStatement(
                        "INSERT INTO dwd_dj_xxly_dzz_detail (wg_code, wg_name, org_code, org_name, org_type, " +
                                "community_name, emerging_filed_type, unit_type, dzzlx_update_time, dzzlx_taskid) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {

                    // 设置批量大小，避免内存问题
                    int batchSize = 1000;
                    int count = 0;

                    for (DzzDetail detail : dzzDetails) {
                        pushStatement.setString(1, detail.getWgCode());
                        pushStatement.setString(2, detail.getWgName());
                        pushStatement.setString(3, detail.getOrgCode());
                        pushStatement.setString(4, detail.getOrgName());
                        pushStatement.setString(5, detail.getOrgType());
                        pushStatement.setString(6, detail.getCommunityName());
                        pushStatement.setString(7, detail.getEmergingFiledType());
                        pushStatement.setString(8, detail.getUnitType());
                        pushStatement.setTimestamp(9, currentTimestamp);  // 使用统一的时间
                        pushStatement.setString(10, taskId);  // 使用统一的taskId
                        pushStatement.addBatch();

                        // 分批执行
                        if (++count % batchSize == 0) {
                            pushStatement.executeBatch();
                            log.info("已批量推送{}条数据", count);
                        }
                    }

                    // 执行剩余批次
                    int[] results = pushStatement.executeBatch();
                    int successCount = Arrays.stream(results).sum();

                    log.info("成功推送{}条党组织数据到{}区域，总计{}条",
                            successCount, districtEnum.getName(), dzzDetails.size());
                }
            }
        } catch (SQLException e) {
            log.error("推送党组织数据到数据库失败，区域: {}",
                    dzzDetails.get(0).getAreaName(), e);
            throw new RuntimeException("推送党组织数据失败", e);
        }
    }


    public void setAreaNameIfContainsDistrict(DzzDetail detail) {
        if (detail != null && detail.getCommunityName() != null) {
            String communityName = detail.getCommunityName();

            // 遍历所有区枚举值
            for (DistrictEnum district : DistrictEnum.values()) {
                if (communityName.contains(district.getName())) {
                    detail.setAreaName(district.getName());
                    return; // 找到匹配后立即返回
                }
            }
        }
    }





}
