package com.dataExtracting.helper;

import com.dataExtracting.domain.entity.SourceDetailObj;
import com.dataExtracting.domain.entity.SourceObj;
import com.dataExtracting.domain.entity.SourceObjV2;
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
    private static final String DM_TARGET_USERNAME = "Front_BIG_DATA_CENTER";
    private static final String DM_TARGET_PASSWORD = "Speacial_Key_For_1dsjzx";
    private static final String DM_TARGET_SCHEMA = "Front_BIG_DATA_CENTER";



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

    public List<SourceObjV2> getSourceInfoV2Data(String username, String password, String schema, String todayStr) {
        String query = todayStr != null
                ? "SELECT * FROM dwd_dj_xxly_info_v2 WHERE (dsjzx_taskid = ? or TO_CHAR(jhpt_update_time, 'yyyyMMdd') = ?) "
                : "SELECT * FROM dwd_dj_xxly_info_v2";

        try (Connection connection = DBConnectionUtils.getRDJCConnection(username, password, schema);
             PreparedStatement statement = connection.prepareStatement(query)) {

            if (todayStr != null) {
                statement.setString(1, todayStr);
                statement.setString(2, todayStr);
            }

            return processResultSetV2(statement.executeQuery());
        } catch (SQLException e) {
            throw new RuntimeException("查询 dwd_dj_xxly_info_v2 表失败: " + e.getMessage(), e);
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

    private List<SourceObjV2> processResultSetV2(ResultSet resultSet) throws SQLException {
        List<SourceObjV2> resultList = new ArrayList<>();
        while (resultSet.next()) {
            SourceObjV2 obj = new SourceObjV2();
            obj.setGridCode(resultSet.getString("grid_code"));
            obj.setGridName(resultSet.getString("grid_name"));
            obj.setSczt(resultSet.getInt("sczt"));
            obj.setCyry(resultSet.getInt("cyry"));
            obj.setFgsczt(resultSet.getInt("fgsczt"));
            obj.setJq(resultSet.getInt("jq"));
            obj.setLy(resultSet.getInt("ly"));
            obj.setYq(resultSet.getInt("yq"));
            obj.setSq(resultSet.getInt("sq"));
            obj.setSqsc(resultSet.getInt("sqsc"));
            obj.setCun(resultSet.getInt("cun"));
            obj.setQt(resultSet.getInt("qt"));
            obj.setDdzjdzz(resultSet.getInt("ddzjdzz"));
            obj.setLhzjdzz(resultSet.getInt("lhzjdzz"));
            obj.setLydzz(resultSet.getInt("lydzz"));
            obj.setYqdzz(resultSet.getInt("yqdzz"));
            obj.setJqdzz(resultSet.getInt("jqdzz"));
            obj.setSqscdzz(resultSet.getInt("sqscdzz"));
            obj.setCdzz(resultSet.getInt("cdzz"));
            obj.setHyzz(resultSet.getInt("hyzz"));
            obj.setLsdzz(resultSet.getInt("lsdzz"));
            obj.setWfg(resultSet.getInt("wfg"));
            obj.setDdzjdzzry(resultSet.getInt("ddzjdzzry"));
            obj.setLhzjdzzry(resultSet.getInt("lhzjdzzry"));
            obj.setLydzzry(resultSet.getInt("lydzzry"));
            obj.setYqdzzry(resultSet.getInt("yqdzzry"));
            obj.setJqdzzry(resultSet.getInt("jqdzzry"));
            obj.setSqscdzzry(resultSet.getInt("sqscdzzry"));
            obj.setCdzzry(resultSet.getInt("cdzzry"));
            obj.setHyzzry(resultSet.getInt("hyzzry"));
            obj.setLsdzzry(resultSet.getInt("lsdzzry"));
            obj.setWfgry(resultSet.getInt("wfgry"));
            obj.setZgmyqy500qwc(resultSet.getInt("zgmyqy500qwc"));
            obj.setZgmyqy500q(resultSet.getInt("zgmyqy500q"));
            obj.setZghlwqy100qwc(resultSet.getInt("zghlwqy100qwc"));
            obj.setZghlwqy100q(resultSet.getInt("zghlwqy100q"));
            obj.setZjtxxjrqywc(resultSet.getInt("zjtxxjrqywc"));
            obj.setZjtxxjrqy(resultSet.getInt("zjtxxjrqy"));
            obj.setJnzbssqywc(resultSet.getInt("jnzbssqywc"));
            obj.setJnzbssqy(resultSet.getInt("jnzbssqy"));
            obj.setDjsqywc(resultSet.getInt("djsqywc"));
            obj.setDjsqy(resultSet.getInt("djsqy"));
            obj.setKjxzxqywc(resultSet.getInt("kjxzxqywc"));
            obj.setKjxzxqy(resultSet.getInt("kjxzxqy"));
            obj.setJymbfqydwwc(resultSet.getInt("jymbfqydwwc"));
            obj.setJymbfqydw(resultSet.getInt("jymbfqydw"));
            obj.setYlwsjgmbfqydwwc(resultSet.getInt("ylwsjgmbfqydwwc"));
            obj.setYlwsjgmbfqydw(resultSet.getInt("ylwsjgmbfqydw"));
            obj.setCpjrjgwc(resultSet.getInt("cpjrjgwc"));
            obj.setCpjrjg(resultSet.getInt("cpjrjg"));
            obj.setHlwqywc(resultSet.getInt("hlwqywc"));
            obj.setHlwqy(resultSet.getInt("hlwqy"));
            obj.setDykjzxqywc(resultSet.getInt("dykjzxqywc"));
            obj.setDykjzxqy(resultSet.getInt("dykjzxqy"));
            obj.setProcessState(resultSet.getString("process_state"));
            obj.setJhptUpdateTime(resultSet.getString("jhpt_update_time"));
            obj.setDsjzxTaskid(resultSet.getString("dsjzx_taskid"));
            resultList.add(obj);
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

    public List<SourceDetailObj> getSourceDetailInfoDataByTime(String username, String password, String schema) {
        String query = "SELECT * FROM dwd_dj_xxly_zf_detail " +
                "WHERE EXTRACT(HOUR FROM jhpt_update_time) = 23";

        try (Connection connection = DBConnectionUtils.getRDJCConnection(username, password, schema);
             PreparedStatement statement = connection.prepareStatement(query)) {
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


    // 从目标库获取最后同步时间
    public Date getLastSyncTimeFromDmTarget() {
        String query = "SELECT MAX(jhpt_update_time) FROM dwd_dj_xxly_info";
        try (Connection conn = DBConnectionUtils.getDMConnection(DM_TARGET_USERNAME, DM_TARGET_PASSWORD, DM_TARGET_SCHEMA);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            return rs.next() ? rs.getTimestamp(1) : null;
        } catch (SQLException e) {
            log.warn("获取目标dm库最后同步时间失败", e);
            return null;
        }
    }

    public Date getLastSyncTimeFromDmTargetV2() {
        String query = "SELECT MAX(jhpt_update_time) FROM dwd_dj_xxly_info_V2";
        try (Connection conn = DBConnectionUtils.getDMConnection(DM_TARGET_USERNAME, DM_TARGET_PASSWORD, DM_TARGET_SCHEMA);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            return rs.next() ? rs.getTimestamp(1) : null;
        } catch (SQLException e) {
            log.warn("获取目标dm库最后同步时间失败", e);
            return null;
        }
    }

    // 从走访达梦目标库获取最后同步时间
    public Date getLastSyncTimeFromDmTargetDetail() {
        String query = "SELECT MAX(jhpt_update_time) FROM dwd_dj_xxly_zf_detail";
        try (Connection conn = DBConnectionUtils.getDMConnection(DM_TARGET_USERNAME, DM_TARGET_PASSWORD, DM_TARGET_SCHEMA);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            return rs.next() ? rs.getTimestamp(1) : null;
        } catch (SQLException e) {
            log.warn("获取目标dm库最后同步时间失败", e);
            return null;
        }
    }

    public Connection getDmTargetConn() {
        Connection conn = null;
        try {
            conn = DBConnectionUtils.getDMConnection(DM_TARGET_USERNAME, DM_TARGET_PASSWORD, DM_TARGET_SCHEMA);
        } catch (SQLException e) {
            log.warn("获取dm目标库连接失败", e);
        }
        return conn;
    }

    public Long getDmCount(String tableName) {
        String query = "SELECT count(*) FROM " + tableName;
        try (Connection conn = DBConnectionUtils.getDMConnection(DM_TARGET_USERNAME, DM_TARGET_PASSWORD, DM_TARGET_SCHEMA);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0L;
        } catch (SQLException e) {
            log.warn("获取dm目标库连接失败", e);
            return null;
        }
    }

    public void truncateDm(String tableName) {
        String sql = "TRUNCATE TABLE " + tableName;
        try (Connection conn = DBConnectionUtils.getDMConnection(DM_TARGET_USERNAME, DM_TARGET_PASSWORD, DM_TARGET_SCHEMA);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            log.warn("执行表截断操作失败: " + tableName, e);
            throw new RuntimeException("执行表截断操作失败", e);
        }
    }




}
