package com.dataGovernance.helper;

import com.dataGovernance.domain.entity.DwdAjWgajInfo1208;
import com.dataGovernance.domain.entity.DwdRlRecord;
import com.dataGovernance.domain.entity.Rxb12345Gongdan06;
import com.dataGovernance.domain.entity.TousuGongdanSWB;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.List;

@Slf4j
@Component
public class JdbcBatchInserter {

    private static final String SQL =
            "INSERT INTO rxb_12345_gongdan_06_tousu (" +
                    "wpid, calltime, callnum, rel_name, gender, rel_district, rel_address, " +
                    "call_type, isrepeat, wp_source, wp_type, class1, class2, class3, class4, " +
                    "summary, supervision, dept_level2, wp_customertype, wp_servicetype, " +
                    "rel_phoneno, hurry_count, priority, note, callid, new_class1, new_class2, " +
                    "new_class3, new_class4, new_class5, dept_level3, create_time, dsjzx_taskid, " +
                    " wpid_ny " +
                    ") " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " +
                    "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";


    private static final String SQL_RL =
            "INSERT INTO dwd_rl_record (" +
                    "grid_code, grid_name, street, area, " +
                    "begin_time, end_time, population" +
                    ") VALUES (?, ?, ?, ?, ?, ?, ?)";

    public void insertBatch(Connection conn, List<Rxb12345Gongdan06> list, int batchSize) throws SQLException {
        if (list == null || list.isEmpty()) return;

        boolean prev = conn.getAutoCommit();
        conn.setAutoCommit(false);

        try (PreparedStatement ps = conn.prepareStatement(SQL)) {

            int count = 0;
            for (Rxb12345Gongdan06 o : list) {

                ps.setString(1, o.getWpid());
                ps.setTimestamp(2, toTs(o.getCalltime()));
                ps.setString(3, o.getCallnum());
                ps.setString(4, o.getRelName());
                ps.setString(5, o.getGender());
                ps.setString(6, o.getRelDistrict());
                ps.setString(7, o.getRelAddress());
                ps.setString(8, o.getCallType());
                ps.setString(9, o.getIsrepeat());
                ps.setString(10, o.getWpSource());
                ps.setString(11, o.getWpType());
                ps.setString(12, o.getClass1());
                ps.setString(13, o.getClass2());
                ps.setString(14, o.getClass3());
                ps.setString(15, o.getClass4());
                ps.setString(16, o.getSummary());
                ps.setString(17, o.getSupervision());
                ps.setString(18, o.getDeptLevel2());
                ps.setString(19, o.getWpCustomertype());
                ps.setString(20, o.getWpServicetype());
                ps.setString(21, o.getRelPhoneno());
                ps.setString(22, o.getHurryCount());
                ps.setString(23, o.getPriority());
                ps.setString(24, o.getNote());
                ps.setString(25, o.getCallid());
                ps.setString(26, o.getNewClass1());
                ps.setString(27, o.getNewClass2());
                ps.setString(28, o.getNewClass3());
                ps.setString(29, o.getNewClass4());
                ps.setString(30, o.getNewClass5());
                ps.setString(31, o.getDeptLevel3());

                ps.setTimestamp(32, toTs(o.getCreateTime()));   // ✔ 正确写入 timestamp

                ps.setString(33, o.getDsjzxTaskid());
                ps.setString(34, o.getWpid().substring(0, 6));

                ps.addBatch();
                count++;

                if (count % batchSize == 0) {
                    ps.executeBatch();
                    conn.commit();
                }
            }

            ps.executeBatch();
            conn.commit();

        } catch (Exception e) {
            conn.rollback();
            log.error("批量插入异常", e);
            throw e;
        } finally {
            conn.setAutoCommit(prev);
        }
    }

    private static final String SQL_DWD =
            "INSERT INTO dsjzx.dwd_aj_wgaj_info_1208 (" +
                    "\"VERIFY_IMG\", \"REPORT_IMG\", \"CONTACT_PHONE\", \"REPORTER\", \"CASE_TYPE\", " +
                    "\"ISSUE_SRC\", \"PROBLEM_DESCRIPTION\", \"Y_COORDINATE\", \"TASK_ID\", \"GRID_CODE\", " +
                    "\"STREET_NAME\", \"VALLAGE_COMMUNITY\", \"DISCOVER_TIME\", \"CASE_STATUS\", \"SETTLE_TIME\", " +
                    "\"OCCURRENCE_ADDRESS\", \"X_COORDINATE\", \"INSERTTIME\", \"updatetime\", \"area_code\", " +
                    "\"area\", \"street_code\", \"grid_name\", \"street\", \"discover_time_ts\", " +
                    "\"source_area\", \"merge_time\", \"ql\", \"qw\", \"zd\", " +
                    "\"wpid\", \"wp_type\", \"discover_time_ym\" " +
                    ") VALUES (" +
                    "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?" +
                    ")";


    public void insertDwdBatch(Connection conn, List<DwdAjWgajInfo1208> list, int batchSize) throws SQLException {

        if (list == null || list.isEmpty()) return;

        boolean prev = conn.getAutoCommit();
        conn.setAutoCommit(false);

        String sql =
                "INSERT INTO dsjzx.dwd_aj_wgaj_info_1208 (" +
                        "\"VERIFY_IMG\", \"REPORT_IMG\", \"CONTACT_PHONE\", \"REPORTER\", \"CASE_TYPE\", " +
                        "\"ISSUE_SRC\", \"PROBLEM_DESCRIPTION\", \"Y_COORDINATE\", \"TASK_ID\", \"GRID_CODE\", " +
                        "\"STREET_NAME\", \"VALLAGE_COMMUNITY\", \"DISCOVER_TIME\", \"CASE_STATUS\", \"SETTLE_TIME\", " +
                        "\"OCCURRENCE_ADDRESS\", \"X_COORDINATE\", \"INSERTTIME\", \"updatetime\", \"area_code\", " +
                        "\"area\", \"street_code\", \"grid_name\", \"street\", \"discover_time_ts\", " +
                        "\"source_area\", \"merge_time\", \"ql\", \"qw\", \"zd\", " +
                        "\"wpid\", \"wp_type\", \"discover_time_ym\" " +
                        ") VALUES (" +
                        "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?" +
                        ")";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            int count = 0;
            for (DwdAjWgajInfo1208 o : list) {

                ps.setString(1, o.getVerifyImg());
                ps.setString(2, o.getReportImg());
                ps.setString(3, o.getContactPhone());
                ps.setString(4, o.getReporter());
                ps.setString(5, o.getCaseType());
                ps.setString(6, o.getIssueSrc());
                ps.setString(7, o.getProblemDescription());
                ps.setString(8, o.getYCoordinate());
                ps.setString(9, o.getTaskId());
                ps.setString(10, o.getGridCode());
                ps.setString(11, o.getStreetName());
                ps.setString(12, o.getVallageCommunity());
                ps.setString(13, o.getDiscoverTime());
                ps.setString(14, o.getCaseStatus());
                ps.setString(15, o.getSettleTime());
                ps.setString(16, o.getOccurrenceAddress());
                ps.setString(17, o.getXCoordinate());
                ps.setTimestamp(18, toTs(o.getInserttime()));
                ps.setTimestamp(19, toTs(o.getUpdatetime()));
                ps.setString(20, o.getAreaCode());
                ps.setString(21, o.getArea());
                ps.setString(22, o.getStreetCode());
                ps.setString(23, o.getGridName());
                ps.setString(24, o.getStreet());
                ps.setTimestamp(25, toTs(o.getDiscoverTimeTs()));
                ps.setString(26, o.getSourceArea());
                ps.setTimestamp(27, toTs(o.getMergeTime()));
                ps.setObject(28, o.getQl());
                ps.setObject(29, o.getQw());
                ps.setObject(30, o.getZd());
                ps.setString(31, o.getWpid());
                ps.setString(32, o.getWpType());
                ps.setString(33, o.getDiscoverTimeYm());

                ps.addBatch();
                count++;

                if (count % batchSize == 0) {
                    ps.executeBatch();
                    conn.commit();
                }
            }

            ps.executeBatch();
            conn.commit();

        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(prev);
        }
    }

    public void insertRlBatch(Connection conn, List<DwdRlRecord> list, int batchSize) throws SQLException {

        if (list == null || list.isEmpty()) return;

        boolean prev = conn.getAutoCommit();
        conn.setAutoCommit(false);

        try (PreparedStatement ps = conn.prepareStatement(SQL_RL)) {

            int count = 0;
            for (DwdRlRecord o : list) {

                ps.setString(1, o.getGridCode());
                ps.setString(2, o.getGridName());
                ps.setString(3, o.getStreet());
                ps.setString(4, o.getArea());

                ps.setLong(5, o.getBeginTime());
                ps.setLong(6, o.getEndTime());
                ps.setInt(7, o.getPopulation());

                ps.addBatch();
                count++;

                if (count % batchSize == 0) {
                    ps.executeBatch();
                    conn.commit();
                }
            }

            ps.executeBatch();
            conn.commit();

        } catch (Exception e) {
            conn.rollback();
            log.error("批量插入人流数据异常", e);
            throw e;
        } finally {
            conn.setAutoCommit(prev);
        }
    }



    private Timestamp toTs(java.util.Date d) {
        return d == null ? null : new Timestamp(d.getTime());
    }

    private static final String SQL_SWB =
            "INSERT INTO rxb_12345_gongdan_06_tousu_swb (" +
                    "wpid, calltime, callnum, rel_name, gender, rel_district, rel_address, " +
                    "call_type, isrepeat, wp_source, wp_type, class1, class2, class3, class4, " +
                    "summary, supervision, dept_level2, wp_customertype, wp_servicetype, " +
                    "rel_phoneno, hurry_count, priority, note, callid, new_class1, new_class2, " +
                    "new_class3, new_class4, new_class5, dept_level3, create_time, dsjzx_taskid) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " +
                    "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    public void insertSwbBatch(Connection conn, List<TousuGongdanSWB> list, int batchSize) throws SQLException {
        if (list == null || list.isEmpty()) return;

        boolean prev = conn.getAutoCommit();
        conn.setAutoCommit(false);

        try (PreparedStatement ps = conn.prepareStatement(SQL_SWB)) {

            int count = 0;
            for (TousuGongdanSWB o : list) {

                ps.setString(1, o.getWpid());
                ps.setTimestamp(2, toTs(o.getCalltime()));
                ps.setString(3, o.getCallnum());
                ps.setString(4, o.getRelName());
                ps.setString(5, o.getGender());
                ps.setString(6, o.getRelDistrict());
                ps.setString(7, o.getRelAddress());
                ps.setString(8, o.getCallType());
                ps.setString(9, o.getIsrepeat());
                ps.setString(10, o.getWpSource());
                ps.setString(11, o.getWpType());
                ps.setString(12, o.getClass1());
                ps.setString(13, o.getClass2());
                ps.setString(14, o.getClass3());
                ps.setString(15, o.getClass4());
                ps.setString(16, o.getSummary());
                ps.setString(17, o.getSupervision());
                ps.setString(18, o.getDeptLevel2());
                ps.setString(19, o.getWpCustomertype());
                ps.setString(20, o.getWpServicetype());
                ps.setString(21, o.getRelPhoneno());
                ps.setString(22, o.getHurryCount());
                ps.setString(23, o.getPriority());
                ps.setString(24, o.getNote());
                ps.setString(25, o.getCallid());
                ps.setString(26, o.getNewClass1());
                ps.setString(27, o.getNewClass2());
                ps.setString(28, o.getNewClass3());
                ps.setString(29, o.getNewClass4());
                ps.setString(30, o.getNewClass5());
                ps.setString(31, o.getDeptLevel3());
                ps.setTimestamp(32, toTs(o.getCreateTime()));
                ps.setString(33, o.getDsjzxTaskid());

                ps.addBatch();
                count++;

                if (count % batchSize == 0) {
                    ps.executeBatch();
                    conn.commit();
                }
            }

            ps.executeBatch();
            conn.commit();

        } catch (Exception e) {
            conn.rollback();
            log.error("批量插入SWB数据异常", e);
            throw e;
        } finally {
            conn.setAutoCommit(prev);
        }
    }
}
