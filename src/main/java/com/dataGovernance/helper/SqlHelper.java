package com.dataGovernance.helper;

import com.dataGovernance.domain.entity.origin.DwdAjWgajInfo;
import com.dataGovernance.domain.entity.origin.SourceObj;
import com.dataGovernance.domain.enums.DistrictEnum;
import com.dataGovernance.utils.DBConnectionUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class SqlHelper {

    private final DataSource rdjcDataSource;

    private Connection cursorConn;
    private boolean cursorOpened = false;

    public SqlHelper(DataSource rdjcDataSource) {
        this.rdjcDataSource = rdjcDataSource;
    }

    /**
     * 打开游标（仅调用一次）
     */
    public void openCursor() throws SQLException {
        if (cursorOpened) return;

        cursorConn = DBConnectionUtils.getRDJCConnection(
                DistrictEnum.DATA_PROCESS.getUsername(),
                DistrictEnum.DATA_PROCESS.getPassword(),
                DistrictEnum.DATA_PROCESS.getSchema());
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
                        "WHERE REPLACE(TRIM(wp_type), '　', '') = '投诉举报类'";

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
                        "WHERE REPLACE(TRIM(wp_type), '　', '') = '投诉举报类'";

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



    /**
     * 关闭游标（迁移完成后调用）
     */
    public void closeCursor() {
        if (!cursorOpened) return;

        try (Statement st = cursorConn.createStatement()) {
            st.execute("CLOSE data_cursor");
            st.execute("COMMIT");
        } catch (Exception e) {
            log.error("关闭游标失败", e);
        }

        try {
            cursorConn.close();
        } catch (Exception ignore) {}

        cursorOpened = false;
        log.info("游标 data_cursor 已关闭");
    }
}
