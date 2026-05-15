package com.dataGovernance.helper;

import com.dataGovernance.domain.entity.SgbXskbShenqing0508;
import com.dataGovernance.domain.entity.origin.DwdAjWgajInfo;
import com.dataGovernance.domain.entity.origin.SgbXskbShenheOrigin;

import com.dataGovernance.domain.entity.origin.SgbXskbZoufangOrigin;
import com.dataGovernance.domain.entity.origin.SourceObj;
import com.dataGovernance.domain.enums.DistrictEnum;
import com.dataGovernance.utils.DBConnectionUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class SqlHelper {

    private final DataSource rdjcDataSource;
    private final DataSource dataSource;

    private Connection cursorConn;
    private boolean cursorOpened = false;
    private String currentCursorName = null;

    public SqlHelper(DataSource rdjcDataSource, DataSource dataSource) {
        this.rdjcDataSource = rdjcDataSource;
        this.dataSource = dataSource;
    }

    /**
     * 打开游标（仅调用一次）
     */
    public void openCursor() throws SQLException {
        if (cursorOpened) return;

        cursorConn = DBConnectionUtils.getConnection(
                DistrictEnum.DSJZX.getUsername(),
                DistrictEnum.DSJZX.getPassword(),
                DistrictEnum.DSJZX.getSchema());
        cursorConn.setAutoCommit(false);

        try (Statement st = cursorConn.createStatement()) {

            // 使用 CTID 顺序读取（最快）
            String sql = "DECLARE data_cursor CURSOR FOR SELECT * FROM rxb_12345_gongdan_06 where wp_type" +
                    " = '投诉举报类' ORDER BY CTID";

            st.execute("BEGIN");
            st.execute(sql);
        }

        cursorOpened = true;
        log.info("游标 data_cursor 已开启");
    }

    /**
     * 获取投诉表中最大的 DSJZX_TASKID
     * 
     * @return 最大的taskId值，如果表为空返回null
     */
    public String getMaxTaskIdFromSource() {
        String sql = "SELECT MAX(\"DSJZX_TASKID\") FROM \"dsjzx\".\"rxb_12345_gongdan_06_tousu_swb\"";

        try (Connection conn = DBConnectionUtils.getConnection(
                DistrictEnum.DSJZX.getUsername(),
                DistrictEnum.DSJZX.getPassword(),
                DistrictEnum.DSJZX.getSchema());
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                String maxTaskId = rs.getString(1);
                log.info("投诉表最大 DSJZX_TASKID: {}", maxTaskId);
                return maxTaskId;
            }

        } catch (Exception e) {
            log.error("查询投诉表最大 DSJZX_TASKID 失败", e);
        }
        return null;
    }

    /**
     * 统计投诉表中满足条件的数据量（用于SWB同步）
     * 
     * @param firstInsert true-全量统计，false-增量统计（根据最大taskId过滤）
     * @return 满足条件的数据行数
     */
    public long countSwbRows(boolean firstInsert) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) FROM \"dsjzx\".\"rxb_12345_gongdan_06_tousu\" A ");
        sql.append("WHERE A.\"DEPT_LEVEL2\" NOT LIKE '%区人民政府' AND A.\"DEPT_LEVEL2\" IS NOT NULL ");

        if (!firstInsert) {
            String maxTaskId = getMaxTaskIdFromSource();
            if (maxTaskId != null && !maxTaskId.isEmpty()) {
                sql.append("AND A.\"DSJZX_TASKID\" > '").append(maxTaskId).append("'");
            }
        }

        try (Connection conn = DBConnectionUtils.getConnection(
                DistrictEnum.DSJZX.getUsername(),
                DistrictEnum.DSJZX.getPassword(),
                DistrictEnum.DSJZX.getSchema());
             PreparedStatement ps = conn.prepareStatement(sql.toString());
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) return rs.getLong(1);

        } catch (Exception e) {
            log.error("统计SWB同步数据量失败", e);
        }
        return 0;
    }

    /**
     * 打开投诉表游标（用于SWB同步）
     * 
     * @param firstInsert true-全量读取，false-增量读取（根据最大taskId过滤）
     * @throws SQLException 数据库操作异常
     */
    public void openCursorWithTaskId(boolean firstInsert) throws SQLException {
        if (cursorOpened) return;

        cursorConn = DBConnectionUtils.getConnection(
                DistrictEnum.DSJZX.getUsername(),
                DistrictEnum.DSJZX.getPassword(),
                DistrictEnum.DSJZX.getSchema());
        cursorConn.setAutoCommit(false);

        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("DECLARE data_cursor CURSOR FOR SELECT * FROM \"dsjzx\".\"rxb_12345_gongdan_06_tousu\" A ");
        sqlBuilder.append("WHERE A.\"DEPT_LEVEL2\" NOT LIKE '%区人民政府' AND A.\"DEPT_LEVEL2\" IS NOT NULL ");

        if (!firstInsert) {
            String maxTaskId = getMaxTaskIdFromSource();
            if (maxTaskId != null && !maxTaskId.isEmpty()) {
                sqlBuilder.append("AND A.\"DSJZX_TASKID\" > '").append(maxTaskId).append("' ");
                log.info("使用投诉表最大taskId作为过滤条件: {}", maxTaskId);
            }
        }

        sqlBuilder.append("ORDER BY CTID");

        try (Statement st = cursorConn.createStatement()) {
            st.execute("BEGIN");
            st.execute(sqlBuilder.toString());
        }

        cursorOpened = true;
        log.info("游标 data_cursor 已开启（首次插入: {}）", firstInsert);
    }

    /**
     * 打开 dwd 表游标（仿照原风格）
     */
    public void openDwdCursor() throws SQLException {
        if (cursorOpened) return;

        cursorConn = DBConnectionUtils.getConnection(
                DistrictEnum.DSJZX.getUsername(),
                DistrictEnum.DSJZX.getPassword(),
                DistrictEnum.DSJZX.getSchema());
        cursorConn.setAutoCommit(false);

        try (Statement st = cursorConn.createStatement()) {

            String sql =
                    "DECLARE dwd_cursor CURSOR FOR " +
                            "SELECT * FROM dsjzx.dwd_aj_wgaj_info ORDER BY CTID";

            st.execute("BEGIN");
            st.execute(sql);
        }

        cursorOpened = true;
        log.info("游标 dwd_cursor 已开启");
    }


    /**
     * 每次 FETCH pageSize 行
     */
    public List<SourceObj> fetchPage(int pageSize) throws SQLException {

        if (!cursorOpened) {
            throw new IllegalStateException("Cursor not opened! Call openCursor() first.");
        }

        List<SourceObj> list = new ArrayList<>();

        String fetchSql = "FETCH " + pageSize + " FROM data_cursor";

        try (PreparedStatement ps = cursorConn.prepareStatement(fetchSql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                SourceObj obj = new SourceObj();

                obj.setWpid(rs.getString("WPID"));
                obj.setCalltime(rs.getString("CALLTIME"));
                obj.setCallnum(rs.getString("CALLNUM"));
                obj.setRelName(rs.getString("REL_NAME"));
                obj.setGender(rs.getString("GENDER"));
                obj.setRelDistrict(rs.getString("REL_DISTRICT"));
                obj.setRelAddress(rs.getString("REL_ADDRESS"));
                obj.setCallType(rs.getString("CALL_TYPE"));
                obj.setIsrepeat(rs.getString("ISREPEAT"));
                obj.setWpSource(rs.getString("WP_SOURCE"));
                obj.setWpType(rs.getString("WP_TYPE"));
                obj.setClass1(rs.getString("CLASS1"));
                obj.setClass2(rs.getString("CLASS2"));
                obj.setClass3(rs.getString("CLASS3"));
                obj.setClass4(rs.getString("CLASS4"));
                obj.setSummary(rs.getString("SUMMARY"));
                obj.setSupervision(rs.getString("SUPERVISION"));
                obj.setDeptLevel2(rs.getString("DEPT_LEVEL2"));
                obj.setWpCustomertype(rs.getString("WP_CUSTOMERTYPE"));
                obj.setWpServicetype(rs.getString("WP_SERVICETYPE"));
                obj.setRelPhoneno(rs.getString("REL_PHONENO"));
                obj.setHurryCount(rs.getString("HURRY_COUNT"));
                obj.setPriority(rs.getString("PRIORITY"));
                obj.setNote(rs.getString("NOTE"));
                obj.setCallid(rs.getString("CALLID"));
                obj.setNewClass1(rs.getString("NEW_CLASS1"));
                obj.setNewClass2(rs.getString("NEW_CLASS2"));
                obj.setNewClass3(rs.getString("NEW_CLASS3"));
                obj.setNewClass4(rs.getString("NEW_CLASS4"));
                obj.setNewClass5(rs.getString("NEW_CLASS5"));
                obj.setDeptLevel3(rs.getString("DEPT_LEVEL3"));

                Timestamp ts = rs.getTimestamp("CREATE_TIME");
                obj.setCreateTime(ts != null ? ts.toString() : null);

                obj.setDsjzxTaskid(rs.getString("DSJZX_TASKID"));

                list.add(obj);
            }
        }

        return list;
    }

    public long countFilteredRows() {

        String sql =
                "SELECT COUNT(*) FROM rxb_12345_gongdan_06 " +
                        "WHERE wp_type = '投诉举报类'";

        try (Connection conn = DBConnectionUtils.getRDJCConnection(
                DistrictEnum.DATA_PROCESS.getUsername(),
                DistrictEnum.DATA_PROCESS.getPassword(),
                DistrictEnum.DATA_PROCESS.getSchema());
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) return rs.getLong(1);

        } catch (Exception e) {
            log.error("统计源库数量失败", e);
        }
        return 0;
    }

    public long countResultRows(Connection conn) {

        String sql =
                "SELECT COUNT(*) FROM rxb_12345_gongdan_06 " +
                        "WHERE wp_type= '投诉举报类'";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getLong(1);
        } catch (Exception e) {
            log.error("统计源库数量失败", e);
        }
        return 0;
    }

    public List<DwdAjWgajInfo> fetchDwdPage(int pageSize) throws SQLException {

        if (!cursorOpened) {
            throw new IllegalStateException("Cursor not opened!");
        }

        List<DwdAjWgajInfo> list = new ArrayList<>();

        String fetchSql = "FETCH " + pageSize + " FROM dwd_cursor";

        try (PreparedStatement ps = cursorConn.prepareStatement(fetchSql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                DwdAjWgajInfo obj = new DwdAjWgajInfo();

                obj.setVerifyImg(rs.getString("VERIFY_IMG"));
                obj.setReportImg(rs.getString("REPORT_IMG"));
                obj.setContactPhone(rs.getString("CONTACT_PHONE"));
                obj.setReporter(rs.getString("REPORTER"));
                obj.setCaseType(rs.getString("CASE_TYPE"));
                obj.setIssueSrc(rs.getString("ISSUE_SRC"));
                obj.setProblemDescription(rs.getString("PROBLEM_DESCRIPTION"));
                obj.setYCoordinate(rs.getString("Y_COORDINATE"));
                obj.setTaskId(rs.getString("TASK_ID"));
                obj.setGridCode(rs.getString("GRID_CODE"));
                obj.setStreetName(rs.getString("STREET_NAME"));
                obj.setVallageCommunity(rs.getString("VALLAGE_COMMUNITY"));
                obj.setDiscoverTime(rs.getString("DISCOVER_TIME"));
                obj.setCaseStatus(rs.getString("CASE_STATUS"));
                obj.setSettleTime(rs.getString("SETTLE_TIME"));
                obj.setOccurrenceAddress(rs.getString("OCCURRENCE_ADDRESS"));
                obj.setXCoordinate(rs.getString("X_COORDINATE"));
                obj.setInserttime(rs.getString("INSERTTIME"));
                obj.setUpdatetime(rs.getString("updatetime"));
                obj.setAreaCode(rs.getString("area_code"));
                obj.setArea(rs.getString("area"));
                obj.setStreetCode(rs.getString("street_code"));
                obj.setGridName(rs.getString("grid_name"));
                obj.setStreet(rs.getString("street"));
                obj.setDiscoverTimeTs(rs.getString("discover_time_ts"));
                obj.setSourceArea(rs.getString("source_area"));
                obj.setMergeTime(rs.getString("merge_time"));
                obj.setQl(rs.getString("ql"));
                obj.setQw(rs.getString("qw"));
                obj.setZd(rs.getString("zd"));
                obj.setWpid(rs.getString("wpid"));
                obj.setWpType(rs.getString("wp_type"));
                obj.setDiscoverTimeYm(rs.getString("discover_time_ym"));

                list.add(obj);
            }
        }

        return list;
    }



    public void openSgbShenheCursor() throws SQLException {
        if (cursorOpened) return;

        cursorConn = DBConnectionUtils.getRDJCConnection(
                DistrictEnum.DATA_PROCESS.getUsername(),
                DistrictEnum.DATA_PROCESS.getPassword(),
                DistrictEnum.DATA_PROCESS.getSchema());
        cursorConn.setAutoCommit(false);

        StringBuilder sqlBuilder = new StringBuilder();
        
        String maxTaskId = getMaxTaskIdFromSgbTarget("SGB_XSKB_SHENHE_0508");
        boolean isFirstInsert = (maxTaskId == null || maxTaskId.isEmpty());
        
        if (isFirstInsert) {
            // 首次同步：使用窗口函数去重，每个HANDLE_ID取DSJZX_TASKID最大的一条
            sqlBuilder.append("DECLARE sgb_shenhe_cursor CURSOR FOR ");
            sqlBuilder.append("WITH ranked_data AS (SELECT *, ROW_NUMBER() OVER(PARTITION BY HANDLE_ID ORDER BY DSJZX_TASKID DESC) AS rn FROM SGB_XSKB_SHENHE_0508 A WHERE A.JHPT_DELETE = '0' AND A.HANDLE_ID IS NOT NULL) ");
            sqlBuilder.append("SELECT * FROM ranked_data WHERE rn = 1 ORDER BY DSJZX_TASKID");
        } else {
            // 增量同步：按DSJZX_TASKID过滤
            sqlBuilder.append("DECLARE sgb_shenhe_cursor CURSOR FOR SELECT * FROM SGB_XSKB_SHENHE_0508 A ");
            sqlBuilder.append("WHERE A.JHPT_DELETE = '0' AND A.HANDLE_ID IS NOT NULL ");
            sqlBuilder.append("AND A.DSJZX_TASKID > '").append(maxTaskId).append("' ORDER BY DSJZX_TASKID");
            log.info("使用审核表最大DSJZX_TASKID作为过滤条件: {}", maxTaskId);
        }

        try (Statement st = cursorConn.createStatement()) {
            st.execute("BEGIN");
            st.execute(sqlBuilder.toString());
        }

        cursorOpened = true;
        currentCursorName = "sgb_shenhe_cursor";
        log.info("游标 sgb_shenhe_cursor 已开启（首次插入: {}）", isFirstInsert);
    }

    public void openSgbShenqingCursor() throws SQLException {
        if (cursorOpened) return;

        cursorConn = DBConnectionUtils.getRDJCConnection(
                DistrictEnum.DATA_PROCESS.getUsername(),
                DistrictEnum.DATA_PROCESS.getPassword(),
                DistrictEnum.DATA_PROCESS.getSchema());
        cursorConn.setAutoCommit(false);

        StringBuilder sqlBuilder = new StringBuilder();
        
        String maxTaskId = getMaxTaskIdFromSgbTarget("SGB_XSKB_SHENQING_0508");
        boolean isFirstInsert = (maxTaskId == null || maxTaskId.isEmpty());
        
        if (isFirstInsert) {
            // 首次同步：使用窗口函数去重，每个PROBLEM_ID取DSJZX_TASKID最大的一条
            sqlBuilder.append("DECLARE sgb_shenqing_cursor CURSOR FOR ");
            sqlBuilder.append("WITH ranked_data AS (SELECT *, ROW_NUMBER() OVER(PARTITION BY PROBLEM_ID ORDER BY DSJZX_TASKID DESC) AS rn FROM SGB_XSKB_SHENQING_0508 A WHERE A.JHPT_DELETE = '0' AND A.PROBLEM_ID IS NOT NULL) ");
            sqlBuilder.append("SELECT * FROM ranked_data WHERE rn = 1 ORDER BY DSJZX_TASKID");
        } else {
            // 增量同步：按DSJZX_TASKID过滤
            sqlBuilder.append("DECLARE sgb_shenqing_cursor CURSOR FOR SELECT * FROM SGB_XSKB_SHENQING_0508 A ");
            sqlBuilder.append("WHERE A.JHPT_DELETE = '0' AND A.PROBLEM_ID IS NOT NULL ");
            sqlBuilder.append("AND A.DSJZX_TASKID > '").append(maxTaskId).append("' ORDER BY DSJZX_TASKID");
            log.info("使用申请表最大DSJZX_TASKID作为过滤条件: {}", maxTaskId);
        }

        try (Statement st = cursorConn.createStatement()) {
            st.execute("BEGIN");
            st.execute(sqlBuilder.toString());
        }

        cursorOpened = true;
        currentCursorName = "sgb_shenqing_cursor";
        log.info("游标 sgb_shenqing_cursor 已开启（首次插入: {}）", isFirstInsert);
    }

    public void openSgbZoufangCursor() throws SQLException {
        if (cursorOpened) return;

        cursorConn = DBConnectionUtils.getRDJCConnection(
                DistrictEnum.DATA_PROCESS.getUsername(),
                DistrictEnum.DATA_PROCESS.getPassword(),
                DistrictEnum.DATA_PROCESS.getSchema());
        cursorConn.setAutoCommit(false);

        StringBuilder sqlBuilder = new StringBuilder();
        
        String maxTaskId = getMaxTaskIdFromSgbTarget("SGB_XSKB_ZOUFANG_0508");
        boolean isFirstInsert = (maxTaskId == null || maxTaskId.isEmpty());
        
        if (isFirstInsert) {
            // 首次同步：使用窗口函数去重，每个VISIT_ID取DSJZX_TASKID最大的一条
            sqlBuilder.append("DECLARE sgb_zoufang_cursor CURSOR FOR ");
            sqlBuilder.append("WITH ranked_data AS (SELECT *, ROW_NUMBER() OVER(PARTITION BY VISIT_ID ORDER BY DSJZX_TASKID DESC) AS rn FROM SGB_XSKB_ZOUFANG_0508 A WHERE A.JHPT_DELETE = '0' AND A.VISIT_ID IS NOT NULL) ");
            sqlBuilder.append("SELECT * FROM ranked_data WHERE rn = 1 ORDER BY DSJZX_TASKID");
        } else {
            // 增量同步：按DSJZX_TASKID过滤
            sqlBuilder.append("DECLARE sgb_zoufang_cursor CURSOR FOR SELECT * FROM SGB_XSKB_ZOUFANG_0508 A ");
            sqlBuilder.append("WHERE A.JHPT_DELETE = '0' AND A.VISIT_ID IS NOT NULL ");
            sqlBuilder.append("AND A.DSJZX_TASKID > '").append(maxTaskId).append("' ORDER BY DSJZX_TASKID");
            log.info("使用走访表最大DSJZX_TASKID作为过滤条件: {}", maxTaskId);
        }

        try (Statement st = cursorConn.createStatement()) {
            st.execute("BEGIN");
            st.execute(sqlBuilder.toString());
        }

        cursorOpened = true;
        currentCursorName = "sgb_zoufang_cursor";
        log.info("游标 sgb_zoufang_cursor 已开启（首次插入: {}）", isFirstInsert);
    }

    private String getMaxTaskIdFromSgbTarget(String targetTable) {
        String sql = "SELECT MAX(\"DSJZX_TASKID\") FROM \"" + targetTable + "\"";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                String maxTaskId = rs.getString(1);
                log.info("{} 最大 DSJZX_TASKID: {}", targetTable, maxTaskId);
                return maxTaskId;
            }

        } catch (Exception e) {
            log.error("查询 {} 最大 DSJZX_TASKID 失败", targetTable, e);
        }
        return null;
    }

    public long countSgbRows(String sourceTable) {
        StringBuilder sql = new StringBuilder();
        String idColumn = "";
        String targetTable = "";
        if (sourceTable.equals("SGB_XSKB_SHENHE_0508")) {
            idColumn = "HANDLE_ID";
            targetTable = "SGB_XSKB_SHENHE_0508";
        } else if (sourceTable.equals("SGB_XSKB_SHENQING_0508")) {
            idColumn = "PROBLEM_ID";
            targetTable = "SGB_XSKB_SHENQING_0508";
        } else if (sourceTable.equals("SGB_XSKB_ZOUFANG_0508")) {
            idColumn = "VISIT_ID";
            targetTable = "SGB_XSKB_ZOUFANG_0508";
        }

        String maxTaskId = getMaxTaskIdFromSgbTarget(targetTable);
        boolean isFirstInsert = (maxTaskId == null || maxTaskId.isEmpty());
        
        if (isFirstInsert) {
            // 首次同步：统计去重后的数量
            sql.append("SELECT COUNT(*) FROM (SELECT DISTINCT ON(").append(idColumn).append(") * FROM ").append(sourceTable).append(" A ");
            sql.append("WHERE A.JHPT_DELETE = '0' AND A.").append(idColumn).append(" IS NOT NULL ");
            sql.append("ORDER BY ").append(idColumn).append(", A.DSJZX_TASKID DESC) AS t");
        } else {
            // 增量同步：按DSJZX_TASKID过滤
            sql.append("SELECT COUNT(*) FROM ").append(sourceTable).append(" A ");
            sql.append("WHERE A.JHPT_DELETE = '0' AND A.").append(idColumn).append(" IS NOT NULL ");
            sql.append("AND A.DSJZX_TASKID > '").append(maxTaskId).append("'");
        }

        try (Connection conn = DBConnectionUtils.getRDJCConnection(
                DistrictEnum.DATA_PROCESS.getUsername(),
                DistrictEnum.DATA_PROCESS.getPassword(),
                DistrictEnum.DATA_PROCESS.getSchema());
             PreparedStatement ps = conn.prepareStatement(sql.toString());
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) return rs.getLong(1);

        } catch (Exception e) {
            log.error("统计SGB表数据量失败", e);
        }
        return 0;
    }

    public List<String> getExistingIds(String targetTable, List<String> ids) throws SQLException {
        String idColumn = "";
        if (targetTable.equals("SGB_XSKB_SHENHE_0508")) {
            idColumn = "HANDLE_ID";
        } else if (targetTable.equals("SGB_XSKB_SHENQING_0508")) {
            idColumn = "PROBLEM_ID";
        } else if (targetTable.equals("SGB_XSKB_ZOUFANG_0508")) {
            idColumn = "VISIT_ID";
        } else {
            return new ArrayList<>();
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT \"").append(idColumn).append("\" FROM \"").append(targetTable).append("\" ");
        sql.append("WHERE \"").append(idColumn).append("\" IN (");
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) sql.append(",");
            sql.append("?");
        }
        sql.append(")");

        List<String> existingIds = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < ids.size(); i++) {
                ps.setString(i + 1, ids.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    existingIds.add(rs.getString(1));
                }
            }
        }

        return existingIds;
    }

    public List<SgbXskbShenheOrigin> fetchSgbShenhePage(int pageSize) throws SQLException {
        if (!cursorOpened) {
            throw new IllegalStateException("Cursor not opened!");
        }

        List<SgbXskbShenheOrigin> list = new ArrayList<>();
        String fetchSql = "FETCH " + pageSize + " FROM sgb_shenhe_cursor";

        try (PreparedStatement ps = cursorConn.prepareStatement(fetchSql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                SgbXskbShenheOrigin obj = new SgbXskbShenheOrigin();
                obj.setCreUserId(rs.getString("CRE_USER_ID"));
                obj.setHandleUserId(rs.getString("HANDLE_USER_ID"));
                obj.setOpUserId(rs.getString("OP_USER_ID"));
                obj.setOpUserName(rs.getString("OP_USER_NAME"));
                obj.setCreUserName(rs.getString("CRE_USER_NAME"));
                obj.setHandleStatus(rs.getString("HANDLE_STATUS"));
                obj.setProblemId(rs.getString("PROBLEM_ID"));
                obj.setHandleRoleId(rs.getString("HANDLE_ROLE_ID"));
                obj.setJhptDelete(rs.getString("JHPT_DELETE"));
                obj.setDataUpdateTime(rs.getString("DATA_UPDATE_TIME"));
                obj.setIsSyncVisit(rs.getString("IS_SYNC_VISIT"));
                obj.setHandlePic(rs.getString("HANDLE_PIC"));
                obj.setNodeName(rs.getString("NODE_NAME"));
                obj.setJhptUpdateTime(rs.getString("JHPT_UPDATE_TIME"));
                obj.setHandleId(rs.getString("HANDLE_ID"));
                obj.setHandleContent(rs.getString("HANDLE_CONTENT"));
                obj.setOpTime(rs.getString("OP_TIME"));
                obj.setDeptId(rs.getString("DEPT_ID"));
                obj.setHandleType(rs.getString("HANDLE_TYPE"));
                obj.setCreTime(rs.getString("CRE_TIME"));
                obj.setDsjzxTaskid(rs.getString("DSJZX_TASKID"));
                list.add(obj);
            }
        }

        return list;
    }

    public List<SgbXskbShenqing0508> fetchSgbShenqingPage(int pageSize) throws SQLException {
        if (!cursorOpened) {
            throw new IllegalStateException("Cursor not opened!");
        }

        List<SgbXskbShenqing0508> list = new ArrayList<>();
        String fetchSql = "FETCH " + pageSize + " FROM sgb_shenqing_cursor";

        try (PreparedStatement ps = cursorConn.prepareStatement(fetchSql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                SgbXskbShenqing0508 obj = new SgbXskbShenqing0508();
                obj.setOpUserName(clean(rs.getString("OP_USER_NAME")));
                obj.setJhptDelete(clean(rs.getString("JHPT_DELETE")));
                obj.setProblemImg(clean(rs.getString("PROBLEM_IMG")));
                obj.setJhptUpdateTime(clean(rs.getString("JHPT_UPDATE_TIME")));
                obj.setOpTime(parseDate(rs.getString("OP_TIME")));
                obj.setPersonPhone(clean(rs.getString("PERSON_PHONE")));
                obj.setProblemTime(parseDate(rs.getString("PROBLEM_TIME")));
                obj.setProblemStatus(clean(rs.getString("PROBLEM_STATUS")));
                String visitCountStr = rs.getString("VISIT_COUNT");
                obj.setVisitCount(visitCountStr != null ? Integer.parseInt(visitCountStr) : null);
                obj.setProblemId(clean(rs.getString("PROBLEM_ID")));
                obj.setJwId(clean(rs.getString("JW_ID")));
                obj.setDataUpdateTime(clean(rs.getString("DATA_UPDATE_TIME")));
                obj.setProblemDescription(clean(rs.getString("PROBLEM_DESCRIPTION")));
                obj.setProblemAddress(clean(rs.getString("PROBLEM_ADDRESS")));
                obj.setProblemTags(clean(rs.getString("PROBLEM_TAGS")));
                obj.setPersonName(clean(rs.getString("PERSON_NAME")));
                obj.setCompletedTime(parseDate(rs.getString("COMPLETED_TIME")));
                obj.setOpUserId(clean(rs.getString("OP_USER_ID")));
                obj.setProblemType(clean(rs.getString("PROBLEM_TYPE")));
                obj.setLastVisitTime(parseDate(rs.getString("LAST_VISIT_TIME")));
                obj.setPersonId(clean(rs.getString("PERSON_ID")));
                obj.setDsjzxTaskid(clean(rs.getString("DSJZX_TASKID")));
                obj.setUpdateTime(new java.util.Date());
                
                list.add(obj);
            }
        }

        return list;
    }

    public List<SgbXskbZoufangOrigin> fetchSgbZoufangPage(int pageSize) throws SQLException {
        if (!cursorOpened) {
            throw new IllegalStateException("Cursor not opened!");
        }

        List<SgbXskbZoufangOrigin> list = new ArrayList<>();
        String fetchSql = "FETCH " + pageSize + " FROM sgb_zoufang_cursor";

        try (PreparedStatement ps = cursorConn.prepareStatement(fetchSql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                SgbXskbZoufangOrigin obj = new SgbXskbZoufangOrigin();
                obj.setCreTime(rs.getString("CRE_TIME"));
                obj.setDataUpdateTime(rs.getString("DATA_UPDATE_TIME"));
                obj.setJhptDelete(rs.getString("JHPT_DELETE"));
                obj.setCreUserId(rs.getString("CRE_USER_ID"));
                obj.setDeptId(rs.getString("DEPT_ID"));
                obj.setVisitSatisfaction(rs.getString("VISIT_SATISFACTION"));
                obj.setProblemId(rs.getString("PROBLEM_ID"));
                obj.setVisitId(rs.getString("VISIT_ID"));
                obj.setJhptUpdateTime(rs.getString("JHPT_UPDATE_TIME"));
                obj.setVisitRemark(rs.getString("VISIT_REMARK"));
                obj.setCreUserName(rs.getString("CRE_USER_NAME"));
                obj.setDsjzxTaskid(rs.getString("DSJZX_TASKID"));
                list.add(obj);
            }
        }

        return list;
    }

    /**
     * 关闭游标（迁移完成后调用）
     */
    public void closeCursor() {
        if (!cursorOpened) return;

        String cursorName = currentCursorName != null ? currentCursorName : "data_cursor";
        
        try (Statement st = cursorConn.createStatement()) {
            st.execute("CLOSE " + cursorName);
            st.execute("COMMIT");
        } catch (Exception e) {
            log.error("关闭游标 {} 失败", cursorName, e);
        }

        try {
            cursorConn.close();
        } catch (Exception ignore) {}

        cursorOpened = false;
        currentCursorName = null;
        log.info("游标 {} 已关闭", cursorName);
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
        
        DateFormat[] formats = {
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"),
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"),
                new SimpleDateFormat("yyyy-MM-dd"),
                new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"),
                new SimpleDateFormat("yyyy/MM/dd")
        };
        
        for (DateFormat format : formats) {
            try {
                return format.parse(str);
            } catch (ParseException ignore) {}
        }
        
        return null;
    }
}
