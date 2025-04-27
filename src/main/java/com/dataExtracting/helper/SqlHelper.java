package com.dataExtracting.helper;

import com.dataExtracting.domain.entity.SourceDetailObj;
import com.dataExtracting.domain.entity.SourceObj;
import com.dataExtracting.service.impl.BaseInfoServiceImpl;
import com.dataExtracting.utils.DBConnectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Service
public class SqlHelper {

    private static final Logger log = LoggerFactory.getLogger(SqlHelper.class);

    private static final String TARGET_USERNAME = "swzzb_dqfwzx";
    private static final String TARGET_PASSWORD = "Swzzb_dqfwzx@2025";
    private static final String TARGET_SCHEMA = "swzzb_dqfwzx";


    public List<SourceObj> getSourceInfoData(String username, String password, String schema, String todayStr) {
        String query = todayStr != null
                ? "SELECT * FROM dwd_dj_xxly_info WHERE (dsjzx_taskid = ? or TO_CHAR(jhpt_update_time, 'yyyyMMdd') = ?) "
                : "SELECT * FROM dwd_dj_xxly_info";

        try (Connection connection = DBConnectionUtils.getRDJCConnection(username, password, schema);
             PreparedStatement statement = connection.prepareStatement(query)) {

            if (todayStr != null) {
                statement.setString(1, todayStr);
                statement.setString(2, todayStr);
            }

            return processResultSet(statement.executeQuery());
        } catch (SQLException e) {
            throw new RuntimeException("查询 dwd_dj_xxly_info 表失败: " + e.getMessage(), e);
        }
    }

    private List<SourceObj> processResultSet(ResultSet resultSet) throws SQLException {
        List<SourceObj> resultList = new ArrayList<>();
        while (resultSet.next()) {
            SourceObj sourceObj = new SourceObj();
            sourceObj.setGridCode(resultSet.getString("grid_code"));
            sourceObj.setGridName(resultSet.getString("grid_name"));
            sourceObj.setSczt(resultSet.getString("sczt"));
            sourceObj.setCyry(resultSet.getString("cyry"));
            sourceObj.setZfll(resultSet.getString("zfll"));
            sourceObj.setJldzz(resultSet.getString("jldzz"));
            sourceObj.setQzdw(resultSet.getString("qzdw"));
            sourceObj.setQzdzz(resultSet.getString("qzdzz"));
            sourceObj.setQzdzb(resultSet.getString("qzdzb"));
            sourceObj.setFgqy(resultSet.getString("fgqy"));
            sourceObj.setSyfgqy(resultSet.getString("syfgqy"));
            sourceObj.setHhsyzqy(resultSet.getString("hhsyzqy"));
            sourceObj.setGtgsh(resultSet.getString("gtgsh"));
            sourceObj.setSqjjhmf(resultSet.getString("sqjjhmf"));
            sourceObj.setShzjzz(resultSet.getString("shzjzz"));
            sourceObj.setMbfqydw(resultSet.getString("mbfqydw"));
            sourceObj.setLssws(resultSet.getString("lssws"));
            sourceObj.setKjssws(resultSet.getString("kjssws"));
            sourceObj.setSwssws(resultSet.getString("swssws"));
            sourceObj.setZcpgjg(resultSet.getString("zcpgjg"));
            sourceObj.setJymbfqydw(resultSet.getString("jymbfqydw"));
            sourceObj.setYlwsjgmbfqydw(resultSet.getString("ylwsjgmbfqydw"));
            sourceObj.setCpjrjg(resultSet.getString("cpjrjg"));
            sourceObj.setDfjrjg(resultSet.getString("dfjrjg"));
            sourceObj.setJq(resultSet.getString("jq"));
            sourceObj.setLy(resultSet.getString("ly"));
            sourceObj.setYq(resultSet.getString("yq"));
            sourceObj.setSqsc(resultSet.getString("sqsc"));
            sourceObj.setCun(resultSet.getString("cun"));
            sourceObj.setDwfg(resultSet.getString("dwfg"));
            sourceObj.setDdzjdzz(resultSet.getString("ddzjdzz"));
            sourceObj.setLhzjdzz(resultSet.getString("lhzjdzz"));
            sourceObj.setQyfg(resultSet.getString("qyfg"));
            sourceObj.setLydzz(resultSet.getString("lydzz"));
            sourceObj.setYqdzz(resultSet.getString("yqdzz"));
            sourceObj.setJqdzz(resultSet.getString("jqdzz"));
            sourceObj.setSqscdzz(resultSet.getString("sqscdzz"));
            sourceObj.setCdzz(resultSet.getString("cdzz"));
            sourceObj.setHyfg(resultSet.getString("hyfg"));
            sourceObj.setWfg(resultSet.getString("wfg"));
            sourceObj.setNmsczts(resultSet.getString("nmsczts"));
            sourceObj.setTyssczts(resultSet.getString("tyssczts"));
            sourceObj.setProcessState(resultSet.getString("process_state"));
            sourceObj.setJhptUpdateTime(resultSet.getString("jhpt_update_time"));
            sourceObj.setDsjzxTaskid(resultSet.getString("dsjzx_taskid"));
            resultList.add(sourceObj);
        }
        return resultList;
    }

    public List<SourceDetailObj> getSourceDetailInfoData(String username, String password, String schema, String todayStr) {
        String query = todayStr != null
                ? "SELECT * FROM dwd_dj_xxly_zf_detail WHERE (dsjzx_taskid = ? or TO_CHAR(jhpt_update_time, 'yyyyMMdd') = ?) "
                : "SELECT * FROM dwd_dj_xxly_zf_detail";

        try (Connection connection = DBConnectionUtils.getRDJCConnection(username, password, schema);
             PreparedStatement statement = connection.prepareStatement(query)) {

            if (todayStr != null) {
                statement.setString(1, todayStr);
                statement.setString(2, todayStr);
            }

            return processDetailResultSet(statement.executeQuery());
        } catch (SQLException e) {
            log.error("查询 dwd_dj_xxly_zf_detail 表失败，返回空列表。错误原因: {}", e.getMessage(), e);
            return Collections.emptyList(); // 返回空列表，避免外层方法中断
        }
    }

    private List<SourceDetailObj> processDetailResultSet(ResultSet resultSet) throws SQLException {
        List<SourceDetailObj> resultList = new ArrayList<>();
        while (resultSet.next()) {
            SourceDetailObj sourceDetailObj = new SourceDetailObj();
            sourceDetailObj.setGridCode(resultSet.getString("grid_code"));
            sourceDetailObj.setGridName(resultSet.getString("grid_name"));
            sourceDetailObj.setShopName(resultSet.getString("shop_name"));
            sourceDetailObj.setShopCode(resultSet.getString("shop_code"));
            sourceDetailObj.setVisitTime(resultSet.getString("visit_time"));
            sourceDetailObj.setVisitNum(resultSet.getString("visit_num"));
            sourceDetailObj.setProcessState(resultSet.getString("process_state"));
            sourceDetailObj.setJhptUpdateTime(resultSet.getString("jhpt_update_time"));
            sourceDetailObj.setDsjzxTaskid(resultSet.getString("dsjzx_taskid"));
            resultList.add(sourceDetailObj);
        }
        return resultList;
    }

    // 从目标库获取最后同步时间
    public Date getLastSyncTimeFromTarget() {
        String query = "SELECT MAX(jhpt_update_time) FROM dwd_dj_xxly_info";
        try (Connection conn = DBConnectionUtils.getRDJCConnection(TARGET_USERNAME, TARGET_PASSWORD, TARGET_SCHEMA);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            return rs.next() ? rs.getTimestamp(1) : null;
        } catch (SQLException e) {
            log.warn("获取目标库最后同步时间失败", e);
            return null;
        }
    }

    // 从走访目标库获取最后同步时间
    public Date getLastSyncTimeFromTargetDetail() {
        String query = "SELECT MAX(jhpt_update_time) FROM dwd_dj_xxly_zf_detail";
        try (Connection conn = DBConnectionUtils.getRDJCConnection(TARGET_USERNAME, TARGET_PASSWORD, TARGET_SCHEMA);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            return rs.next() ? rs.getTimestamp(1) : null;
        } catch (SQLException e) {
            log.warn("获取目标库最后同步时间失败", e);
            return null;
        }
    }

    public Connection getTargetConn() {
        Connection conn = null;
        try {
            conn = DBConnectionUtils.getRDJCConnection(TARGET_USERNAME, TARGET_PASSWORD, TARGET_SCHEMA);
        } catch (SQLException e) {
            log.warn("获取目标库连接失败", e);
        }
        return conn;
    }




}
