package com.dataGovernance.helper;

import com.dataGovernance.domain.entity.DwdAjWgajInfo1208;
import com.dataGovernance.domain.entity.DwdRlRecord;
import com.dataGovernance.domain.entity.Rxb12345Gongdan06;
import com.dataGovernance.domain.entity.SgbXskbShenhe0508;
import com.dataGovernance.domain.entity.SgbXskbShenqing0508;
import com.dataGovernance.domain.entity.SgbXskbZoufang0508;
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

    private static final String SQL_SGB_SHENHE_INSERT =
            "INSERT INTO \"SGB_XSKB_SHENHE_0508\" (" +
                    "\"CRE_USER_ID\", \"HANDLE_USER_ID\", \"OP_USER_ID\", \"OP_USER_NAME\", \"CRE_USER_NAME\", " +
                    "\"HANDLE_STATUS\", \"PROBLEM_ID\", \"HANDLE_ROLE_ID\", \"JHPT_DELETE\", \"DATA_UPDATE_TIME\", " +
                    "\"IS_SYNC_VISIT\", \"HANDLE_PIC\", \"NODE_NAME\", \"JHPT_UPDATE_TIME\", \"HANDLE_ID\", " +
                    "\"HANDLE_CONTENT\", \"OP_TIME\", \"DEPT_ID\", \"HANDLE_TYPE\", \"CRE_TIME\", \"DSJZX_TASKID\"" +
                    ") VALUES (" +
                    "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?" +
                    ")";

    private static final String SQL_SGB_SHENHE_UPDATE =
            "UPDATE \"SGB_XSKB_SHENHE_0508\" SET " +
                    "\"CRE_USER_ID\" = ?, \"HANDLE_USER_ID\" = ?, \"OP_USER_ID\" = ?, \"OP_USER_NAME\" = ?, \"CRE_USER_NAME\" = ?, " +
                    "\"HANDLE_STATUS\" = ?, \"PROBLEM_ID\" = ?, \"HANDLE_ROLE_ID\" = ?, \"JHPT_DELETE\" = ?, \"DATA_UPDATE_TIME\" = ?, " +
                    "\"IS_SYNC_VISIT\" = ?, \"HANDLE_PIC\" = ?, \"NODE_NAME\" = ?, \"JHPT_UPDATE_TIME\" = ?, " +
                    "\"HANDLE_CONTENT\" = ?, \"OP_TIME\" = ?, \"DEPT_ID\" = ?, \"HANDLE_TYPE\" = ?, \"CRE_TIME\" = ?, \"DSJZX_TASKID\" = ? " +
                    "WHERE \"HANDLE_ID\" = ?";

    public void insertSgbShenheBatch(Connection conn, List<SgbXskbShenhe0508> list, int batchSize) throws SQLException {
        if (list == null || list.isEmpty()) return;

        boolean prev = conn.getAutoCommit();
        conn.setAutoCommit(false);

        try (PreparedStatement ps = conn.prepareStatement(SQL_SGB_SHENHE_INSERT)) {

            int count = 0;
            for (SgbXskbShenhe0508 o : list) {
                ps.setString(1, o.getCreUserId());
                ps.setString(2, o.getHandleUserId());
                ps.setString(3, o.getOpUserId());
                ps.setString(4, o.getOpUserName());
                ps.setString(5, o.getCreUserName());
                ps.setString(6, o.getHandleStatus());
                ps.setString(7, o.getProblemId());
                ps.setString(8, o.getHandleRoleId());
                ps.setString(9, o.getJhptDelete());
                ps.setString(10, o.getDataUpdateTime());
                ps.setString(11, o.getIsSyncVisit());
                ps.setString(12, o.getHandlePic());
                ps.setString(13, o.getNodeName());
                ps.setString(14, o.getJhptUpdateTime());
                ps.setString(15, o.getHandleId());
                ps.setString(16, o.getHandleContent());
                ps.setTimestamp(17, toTs(o.getOpTime()));
                ps.setString(18, o.getDeptId());
                ps.setString(19, o.getHandleType());
                ps.setTimestamp(20, toTs(o.getCreTime()));
                ps.setString(21, o.getDsjzxTaskid());

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
            log.error("批量插入SGB审核表数据异常", e);
            throw e;
        } finally {
            conn.setAutoCommit(prev);
        }
    }

    public void updateSgbShenheBatch(Connection conn, List<SgbXskbShenhe0508> list, int batchSize) throws SQLException {
        if (list == null || list.isEmpty()) return;

        boolean prev = conn.getAutoCommit();
        conn.setAutoCommit(false);

        try (PreparedStatement ps = conn.prepareStatement(SQL_SGB_SHENHE_UPDATE)) {

            int count = 0;
            for (SgbXskbShenhe0508 o : list) {
                ps.setString(1, o.getCreUserId());
                ps.setString(2, o.getHandleUserId());
                ps.setString(3, o.getOpUserId());
                ps.setString(4, o.getOpUserName());
                ps.setString(5, o.getCreUserName());
                ps.setString(6, o.getHandleStatus());
                ps.setString(7, o.getProblemId());
                ps.setString(8, o.getHandleRoleId());
                ps.setString(9, o.getJhptDelete());
                ps.setString(10, o.getDataUpdateTime());
                ps.setString(11, o.getIsSyncVisit());
                ps.setString(12, o.getHandlePic());
                ps.setString(13, o.getNodeName());
                ps.setString(14, o.getJhptUpdateTime());
                ps.setString(15, o.getHandleContent());
                ps.setTimestamp(16, toTs(o.getOpTime()));
                ps.setString(17, o.getDeptId());
                ps.setString(18, o.getHandleType());
                ps.setTimestamp(19, toTs(o.getCreTime()));
                ps.setString(20, o.getDsjzxTaskid());
                ps.setString(21, o.getHandleId());

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
            log.error("批量更新SGB审核表数据异常", e);
            throw e;
        } finally {
            conn.setAutoCommit(prev);
        }
    }

    private static final String SQL_SGB_SHENQING_INSERT =
            "INSERT INTO \"SGB_XSKB_SHENQING_0508\" (" +
                    "\"OP_USER_NAME\", \"JHPT_DELETE\", \"PROBLEM_IMG\", \"JHPT_UPDATE_TIME\", \"OP_TIME\", " +
                    "\"PERSON_PHONE\", \"PROBLEM_TIME\", \"PROBLEM_STATUS\", \"VISIT_COUNT\", \"PROBLEM_ID\", " +
                    "\"JW_ID\", \"DATA_UPDATE_TIME\", \"PROBLEM_DESCRIPTION\", \"PROBLEM_ADDRESS\", \"PROBLEM_TAGS\", " +
                    "\"PERSON_NAME\", \"COMPLETED_TIME\", \"OP_USER_ID\", \"PROBLEM_TYPE\", \"LAST_VISIT_TIME\", " +
                    "\"PERSON_ID\", \"DSJZX_TASKID\", \"GRID_CODE\", \"GRID_NAME\", \"AREA_CODE\", \"AREA_NAME\", " +
                    "\"STREET_CODE\", \"STREET_NAME\", \"UPDATE_TIME\"" +
                    ") VALUES (" +
                    "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?" +
                    ")";

    private static final String SQL_SGB_SHENQING_UPDATE =
            "UPDATE \"SGB_XSKB_SHENQING_0508\" SET " +
                    "\"OP_USER_NAME\" = ?, \"JHPT_DELETE\" = ?, \"PROBLEM_IMG\" = ?, \"JHPT_UPDATE_TIME\" = ?, \"OP_TIME\" = ?, " +
                    "\"PERSON_PHONE\" = ?, \"PROBLEM_TIME\" = ?, \"PROBLEM_STATUS\" = ?, \"VISIT_COUNT\" = ?, " +
                    "\"JW_ID\" = ?, \"DATA_UPDATE_TIME\" = ?, \"PROBLEM_DESCRIPTION\" = ?, \"PROBLEM_ADDRESS\" = ?, \"PROBLEM_TAGS\" = ?, " +
                    "\"PERSON_NAME\" = ?, \"COMPLETED_TIME\" = ?, \"OP_USER_ID\" = ?, \"PROBLEM_TYPE\" = ?, \"LAST_VISIT_TIME\" = ?, " +
                    "\"PERSON_ID\" = ?, \"DSJZX_TASKID\" = ?, \"GRID_CODE\" = ?, \"GRID_NAME\" = ?, \"AREA_CODE\" = ?, \"AREA_NAME\" = ?, " +
                    "\"STREET_CODE\" = ?, \"STREET_NAME\" = ?, \"UPDATE_TIME\" = ? " +
                    "WHERE \"PROBLEM_ID\" = ?";

    public void insertSgbShenqingBatch(Connection conn, List<SgbXskbShenqing0508> list, int batchSize) throws SQLException {
        if (list == null || list.isEmpty()) return;

        boolean prev = conn.getAutoCommit();
        conn.setAutoCommit(false);

        try (PreparedStatement ps = conn.prepareStatement(SQL_SGB_SHENQING_INSERT)) {

            int count = 0;
            for (SgbXskbShenqing0508 o : list) {
                ps.setString(1, o.getOpUserName());
                ps.setString(2, o.getJhptDelete());
                ps.setString(3, o.getProblemImg());
                ps.setString(4, o.getJhptUpdateTime());
                ps.setTimestamp(5, toTs(o.getOpTime()));
                ps.setString(6, o.getPersonPhone());
                ps.setTimestamp(7, toTs(o.getProblemTime()));
                ps.setString(8, o.getProblemStatus());
                ps.setObject(9, o.getVisitCount());
                ps.setString(10, o.getProblemId());
                ps.setString(11, o.getJwId());
                ps.setString(12, o.getDataUpdateTime());
                ps.setString(13, o.getProblemDescription());
                ps.setString(14, o.getProblemAddress());
                ps.setString(15, o.getProblemTags());
                ps.setString(16, o.getPersonName());
                ps.setTimestamp(17, toTs(o.getCompletedTime()));
                ps.setString(18, o.getOpUserId());
                ps.setString(19, o.getProblemType());
                ps.setTimestamp(20, toTs(o.getLastVisitTime()));
                ps.setString(21, o.getPersonId());
                ps.setString(22, o.getDsjzxTaskid());
                ps.setString(23, o.getGridCode());
                ps.setString(24, o.getGridName());
                ps.setString(25, o.getAreaCode());
                ps.setString(26, o.getAreaName());
                ps.setString(27, o.getStreetCode());
                ps.setString(28, o.getStreetName());
                ps.setTimestamp(29, toTs(o.getUpdateTime()));

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
            log.error("批量插入SGB申请表数据异常", e);
            throw e;
        } finally {
            conn.setAutoCommit(prev);
        }
    }

    public void updateSgbShenqingBatch(Connection conn, List<SgbXskbShenqing0508> list, int batchSize) throws SQLException {
        if (list == null || list.isEmpty()) return;

        boolean prev = conn.getAutoCommit();
        conn.setAutoCommit(false);

        try (PreparedStatement ps = conn.prepareStatement(SQL_SGB_SHENQING_UPDATE)) {

            int count = 0;
            for (SgbXskbShenqing0508 o : list) {
                ps.setString(1, o.getOpUserName());
                ps.setString(2, o.getJhptDelete());
                ps.setString(3, o.getProblemImg());
                ps.setString(4, o.getJhptUpdateTime());
                ps.setTimestamp(5, toTs(o.getOpTime()));
                ps.setString(6, o.getPersonPhone());
                ps.setTimestamp(7, toTs(o.getProblemTime()));
                ps.setString(8, o.getProblemStatus());
                ps.setObject(9, o.getVisitCount());
                ps.setString(10, o.getJwId());
                ps.setString(11, o.getDataUpdateTime());
                ps.setString(12, o.getProblemDescription());
                ps.setString(13, o.getProblemAddress());
                ps.setString(14, o.getProblemTags());
                ps.setString(15, o.getPersonName());
                ps.setTimestamp(16, toTs(o.getCompletedTime()));
                ps.setString(17, o.getOpUserId());
                ps.setString(18, o.getProblemType());
                ps.setTimestamp(19, toTs(o.getLastVisitTime()));
                ps.setString(20, o.getPersonId());
                ps.setString(21, o.getDsjzxTaskid());
                ps.setString(22, o.getGridCode());
                ps.setString(23, o.getGridName());
                ps.setString(24, o.getAreaCode());
                ps.setString(25, o.getAreaName());
                ps.setString(26, o.getStreetCode());
                ps.setString(27, o.getStreetName());
                ps.setTimestamp(28, toTs(o.getUpdateTime()));
                ps.setString(29, o.getProblemId());

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
            log.error("批量更新SGB申请表数据异常", e);
            throw e;
        } finally {
            conn.setAutoCommit(prev);
        }
    }

    private static final String SQL_SGB_ZOUFANG_INSERT =
            "INSERT INTO \"SGB_XSKB_ZOUFANG_0508\" (" +
                    "\"CRE_TIME\", \"DATA_UPDATE_TIME\", \"JHPT_DELETE\", \"CRE_USER_ID\", \"DEPT_ID\", " +
                    "\"VISIT_SATISFACTION\", \"PROBLEM_ID\", \"VISIT_ID\", \"JHPT_UPDATE_TIME\", \"VISIT_REMARK\", " +
                    "\"CRE_USER_NAME\", \"DSJZX_TASKID\"" +
                    ") VALUES (" +
                    "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?" +
                    ")";

    private static final String SQL_SGB_ZOUFANG_UPDATE =
            "UPDATE \"SGB_XSKB_ZOUFANG_0508\" SET " +
                    "\"CRE_TIME\" = ?, \"DATA_UPDATE_TIME\" = ?, \"JHPT_DELETE\" = ?, \"CRE_USER_ID\" = ?, \"DEPT_ID\" = ?, " +
                    "\"VISIT_SATISFACTION\" = ?, \"PROBLEM_ID\" = ?, \"JHPT_UPDATE_TIME\" = ?, \"VISIT_REMARK\" = ?, " +
                    "\"CRE_USER_NAME\" = ?, \"DSJZX_TASKID\" = ? " +
                    "WHERE \"VISIT_ID\" = ?";

    public void insertSgbZoufangBatch(Connection conn, List<SgbXskbZoufang0508> list, int batchSize) throws SQLException {
        if (list == null || list.isEmpty()) return;

        boolean prev = conn.getAutoCommit();
        conn.setAutoCommit(false);

        try (PreparedStatement ps = conn.prepareStatement(SQL_SGB_ZOUFANG_INSERT)) {

            int count = 0;
            for (SgbXskbZoufang0508 o : list) {
                ps.setTimestamp(1, toTs(o.getCreTime()));
                ps.setString(2, o.getDataUpdateTime());
                ps.setString(3, o.getJhptDelete());
                ps.setString(4, o.getCreUserId());
                ps.setString(5, o.getDeptId());
                ps.setString(6, o.getVisitSatisfaction());
                ps.setString(7, o.getProblemId());
                ps.setString(8, o.getVisitId());
                ps.setString(9, o.getJhptUpdateTime());
                ps.setString(10, o.getVisitRemark());
                ps.setString(11, o.getCreUserName());
                ps.setString(12, o.getDsjzxTaskid());

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
            log.error("批量插入SGB走访表数据异常", e);
            throw e;
        } finally {
            conn.setAutoCommit(prev);
        }
    }

    public void updateSgbZoufangBatch(Connection conn, List<SgbXskbZoufang0508> list, int batchSize) throws SQLException {
        if (list == null || list.isEmpty()) return;

        boolean prev = conn.getAutoCommit();
        conn.setAutoCommit(false);

        try (PreparedStatement ps = conn.prepareStatement(SQL_SGB_ZOUFANG_UPDATE)) {

            int count = 0;
            for (SgbXskbZoufang0508 o : list) {
                ps.setTimestamp(1, toTs(o.getCreTime()));
                ps.setString(2, o.getDataUpdateTime());
                ps.setString(3, o.getJhptDelete());
                ps.setString(4, o.getCreUserId());
                ps.setString(5, o.getDeptId());
                ps.setString(6, o.getVisitSatisfaction());
                ps.setString(7, o.getProblemId());
                ps.setString(8, o.getJhptUpdateTime());
                ps.setString(9, o.getVisitRemark());
                ps.setString(10, o.getCreUserName());
                ps.setString(11, o.getDsjzxTaskid());
                ps.setString(12, o.getVisitId());

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
            log.error("批量更新SGB走访表数据异常", e);
            throw e;
        } finally {
            conn.setAutoCommit(prev);
        }
    }
}
