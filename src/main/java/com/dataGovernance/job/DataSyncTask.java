package com.dataGovernance.job;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.dataGovernance.domain.entity.DwdRlRecord;
import com.dataGovernance.domain.entity.GridConfig;
import com.dataGovernance.helper.JdbcBatchInserter;
import com.dataGovernance.utils.DBConnectionUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class DataSyncTask {

    private final JdbcBatchInserter jdbcBatchInserter;
    private static final Logger log = LoggerFactory.getLogger(DataSyncTask.class);

    private static final DateTimeFormatter FMT_YMD_HMS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FMT_CN_MD_H = DateTimeFormatter.ofPattern("MM月dd号H点");

    // 正则表达式提取 @1 和 @2 后的内容
    private static final Pattern AI_RESULT_PATTERN = Pattern.compile("@1\\.(.*?)@2\\.(.*)", Pattern.DOTALL);

    // ==========================================
    // 1. 定义网格配置列表 (集中管理4个网格)
    // ==========================================
    private static final List<GridConfig> TARGET_GRIDS = Arrays.asList(
            new GridConfig("4151795597343053249", "ZH31011590201", "国际旅游度假区综合网格", "上海国际旅游度假区", "浦东新区"),
            new GridConfig("4151772507108546469", "ZH31010101301", "第一综合网格", "外滩街道", "黄浦区"),
            new GridConfig("4151772519818439747", "ZH31010101302", "第二综合网格", "外滩街道", "黄浦区"),
            new GridConfig("4151772520350577084", "ZH31010101303", "第三综合网格", "外滩街道", "黄浦区")
    );

    /**
     * 任务一：数据同步入库
     * Cron: 每小时的 10 分执行
     */
    @Scheduled(cron = "0 10 * * * ?")
    public void syncCurrentHourData() {
        long startTaskTime = System.currentTimeMillis();
        log.info(">>>>>> [数据同步] 任务触发，开始处理 {} 个网格 <<<<<<", TARGET_GRIDS.size());

        // 计算时间窗口 (当前小时 14:10 -> 抓取 13:00-14:00)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTimeWindow = now.withMinute(0).withSecond(0).withNano(0);
        LocalDateTime startTimeWindow = endTimeWindow.minusHours(1);

        long beginTs = startTimeWindow.atZone(ZoneId.systemDefault()).toEpochSecond();
        long endTs = endTimeWindow.atZone(ZoneId.systemDefault()).toEpochSecond();

        log.info("同步目标时段: {} -> {}", startTimeWindow, endTimeWindow);

        // 循环处理每个网格
        for (GridConfig grid : TARGET_GRIDS) {
            try {
                syncSingleGrid(grid, beginTs, endTs);
            } catch (Exception e) {
                log.error("网格 [{}] 同步失败", grid.getGridName(), e);
            }
        }

        log.info(">>>>>> [数据同步] 任务结束，耗时 {} ms <<<<<<", System.currentTimeMillis() - startTaskTime);
    }

    /**
     * 任务二：智能分析
     * Cron: 每小时的 15 分执行
     */
    @Scheduled(cron = "0 15 * * * ?")
    public void analyzeTrafficData() {
        long start = System.currentTimeMillis();
        log.info(">>>>>> [智能分析] 任务触发，开始分析 {} 个网格 <<<<<<", TARGET_GRIDS.size());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime targetTime = now.minusHours(1).withMinute(0).withSecond(0).withNano(0);
        long targetTs = toTs(targetTime);

        log.info("分析目标时间点: {}", targetTime.format(FMT_YMD_HMS));

        try (Connection conn = DBConnectionUtils.getDMConnection("skyt_slxx_dghy_prod",
                "Skyt_slxx_dghy_prod@2025", "skyt_slxx_dghy_prod")) {

            // 循环分析每个网格
            for (GridConfig grid : TARGET_GRIDS) {
                try {
                    analyzeSingleGrid(conn, grid, targetTime, targetTs);
                } catch (Exception e) {
                    log.error("网格 [{}] 分析失败", grid.getGridName(), e);
                }
            }

        } catch (Exception e) {
            log.error("[智能分析] 数据库连接异常", e);
        }

        log.info(">>>>>> [智能分析] 任务结束，耗时 {} ms <<<<<<", System.currentTimeMillis() - start);
    }

    // ==========================================
    // 私有方法：同步单个网格逻辑
    // ==========================================
    private void syncSingleGrid(GridConfig grid, long beginTs, long endTs) {
        List<DwdRlRecord> buffer = new ArrayList<>();
        try {
            JSONObject body = new JSONObject();
            body.put("id", grid.getApiId()); // 使用配置的 ID
            body.put("begin", beginTs);
            body.put("end", endTs);
            body.put("interval", 60);
            body.put("people_type", "all");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("jcmp-token", "6e70a7620bda40109c7f4066c6d9f240");

            HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);
            RestTemplate restTemplate = new RestTemplate();

            String resp = restTemplate.postForObject(
                    "https://service-api.onemap.sh.cegn.cn/bigdata/realtime/v1.1/population",
                    entity, String.class
            );

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(resp);
            JsonNode arr = root.path("data");

            if (arr.isEmpty()) {
                log.warn("[{}] 该时段无数据返回", grid.getGridName());
                return;
            }

            for (JsonNode o : arr) {
                String timeStr = o.get("time").asText();
                int value = o.get("value").asInt();
                long recordTs = LocalDateTime.parse(timeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        .atZone(ZoneId.systemDefault()).toEpochSecond();

                if (recordTs < beginTs || recordTs >= endTs) continue;

                DwdRlRecord r = new DwdRlRecord();
                r.setGridCode(grid.getGridCode()); // 使用配置的代码
                r.setGridName(grid.getGridName());
                r.setStreet(grid.getStreet());
                r.setArea(grid.getArea());
                r.setBeginTime(recordTs);
                r.setEndTime(recordTs + 3600);
                r.setPopulation(value);
                buffer.add(r);
            }

            if (!buffer.isEmpty()) {
                try (Connection conn = DBConnectionUtils.getDMConnection("skyt_slxx_dghy_prod",
                        "Skyt_slxx_dghy_prod@2025", "skyt_slxx_dghy_prod")) {
                    jdbcBatchInserter.insertRlBatch(conn, buffer, 500);
                    log.info("[{}] 同步入库成功，共 {} 条", grid.getGridName(), buffer.size());
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("API调用或入库异常", e);
        }
    }

    // ==========================================
    // 私有方法：分析单个网格逻辑
    // ==========================================
    public void analyzeSingleGrid(Connection conn, GridConfig grid, LocalDateTime targetTime, long targetTs) {
        // 1. 查基础信息 (必须带上 gridCode，防止查错人)
        DwdRlRecord targetRecord = getBaseRecord(conn, targetTs, grid.getGridCode());
        if (targetRecord == null) {
            log.warn("[{}] 未找到时间点 [{}] 的数据，跳过。", grid.getGridName(), targetTime);
            return;
        }

        // 2. 查历史数据 (必须带上 gridCode)
        String historyJson = getHistoryDataJson(conn, targetTime, grid.getGridCode());
        if (historyJson == null) {
            log.warn("[{}] 历史对比数据不足，跳过。", grid.getGridName());
            return;
        }
//        log.info("历史数据：{}", historyJson);

        // 3. 组装 Prompt
        String timeCn = targetTime.format(FMT_CN_MD_H);
        String prompt = buildPrompt(historyJson, timeCn);

        // 4. 调用 AI
        String aiRawResp = callQwenAI(prompt);

        // 5. 解析结果并入库
        saveWarningResult(conn, targetRecord, aiRawResp);
    }

    // ================== 数据库与工具方法 (已升级支持多网格) ==================

    private void saveWarningResult(Connection conn, DwdRlRecord record, String aiRawResp) {
        AiResult result = parseAiResponse(aiRawResp);

        log.info("\n====== [{}] 分析报告 ======\n时间: {}\n结果: {}\n状态: {}\n==============================",
                record.getGridName(), record.getBeginTime(), result.description, result.status == 1 ? "异常" : "正常");

        String insertSql = "INSERT INTO dwd_rl_warning (record_id, theme, warning, status, create_time) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            ps.setInt(1, record.getId());
            ps.setString(2, "人流预警");
            ps.setString(3, result.description);
            ps.setInt(4, result.status);
            ps.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("预警入库失败", e);
        }
    }

    /**
     * ⚠️ 关键修改：增加了 gridCode 参数，SQL 增加 AND grid_code = ?
     */
    private DwdRlRecord getBaseRecord(Connection conn, long ts, String gridCode) {
        String sql = "SELECT id, grid_code, grid_name, street, area, begin_time, end_time FROM dwd_rl_record WHERE begin_time = ? AND grid_code = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, ts);
            ps.setString(2, gridCode); // 绑定网格代码
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    DwdRlRecord r = new DwdRlRecord();
                    r.setId(rs.getInt("id"));
                    r.setGridCode(rs.getString("grid_code"));
                    r.setGridName(rs.getString("grid_name"));
                    r.setBeginTime(rs.getLong("begin_time"));
                    return r;
                }
            }
        } catch (SQLException e) {
            log.error("查询基础记录失败", e);
        }
        return null;
    }

    /**
     * ⚠️ 关键修改：增加了 gridCode 参数，SQL 增加 AND grid_code = ?
     */
    private String getHistoryDataJson(Connection conn, LocalDateTime targetTime, String gridCode) {

        // ================== 1. 构造时间戳 ==================
        Set<Long> allTimestamps = new LinkedHashSet<>();

        // 最近 7 天同一时间点
        Set<Long> historySameTimeTs = new HashSet<>();
        for (int i = 1; i <= 7; i++) {
            long ts = toTs(targetTime.minusDays(i));
            historySameTimeTs.add(ts);
            allTimestamps.add(ts);
        }

        // 当天 0 点 → 设定时间点（包含）
        Set<Long> todayTrendTs = new HashSet<>();
        LocalDateTime loop = targetTime.toLocalDate().atStartOfDay();
        while (!loop.isAfter(targetTime)) {
            long ts = toTs(loop);
            todayTrendTs.add(ts);
            allTimestamps.add(ts);
            loop = loop.plusHours(1);
        }

        // ================== 2. 查询 ==================
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        ArrayNode historySameTimeArr = mapper.createArrayNode();
        ArrayNode todayTrendArr = mapper.createArrayNode();

        String sql = buildInSql(allTimestamps.size());

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            int idx = 1;
            for (Long ts : allTimestamps) {
                ps.setLong(idx++, ts);
            }
            ps.setString(idx, gridCode);

            try (ResultSet rs = ps.executeQuery()) {

                int seqHistory = 1; // 最近7天同时间
                int seqToday = 1;   // 当天趋势

                while (rs.next()) {

                    long beginTime = rs.getLong("begin_time");

                    ObjectNode node = mapper.createObjectNode();
                    String tStr = LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochSecond(beginTime),
                            ZoneId.systemDefault()
                    ).format(FMT_YMD_HMS);

                    node.put("时间点", tStr);
                    node.put("grid_name", rs.getString("grid_name"));
                    node.put("population", rs.getInt("population"));

                    // ===== 判断归属 =====
                    if (historySameTimeTs.contains(beginTime)) {
                        node.put("历史序号", seqHistory++);
                        historySameTimeArr.add(node);
                    } else if (todayTrendTs.contains(beginTime)) {
                        node.put("当日序号", seqToday++);
                        todayTrendArr.add(node);
                    }
                }
            }

        } catch (SQLException e) {
            log.error("查询历史数据失败", e);
            return null;
        }

        // ================== 3. 组装结果 ==================
        root.set("历史数据", historySameTimeArr);
        root.set("当日数据", todayTrendArr);

        return (historySameTimeArr.isEmpty() && todayTrendArr.isEmpty())
                ? null
                : root.toString();
    }



    // ⚠️ 关键修改：SQL 尾部追加 AND grid_code = ?
    private String buildInSql(int count) {
        StringBuilder sb = new StringBuilder("SELECT begin_time, grid_name, population FROM dwd_rl_record WHERE begin_time IN (");
        for (int i = 0; i < count; i++) sb.append(i == 0 ? "?" : ",?");
        sb.append(") AND grid_code = ? ORDER BY begin_time ASC");
        return sb.toString();
    }

    private AiResult parseAiResponse(String jsonResp) {
        AiResult result = new AiResult();
        result.description = "AI解析失败";
        result.status = 0;
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonResp);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                String content = choices.get(0).path("message").path("content").asText();
                log.info("AI返回结果：" + content);
                content = content.replaceAll("(?s)<think>.*?</think>", "").trim();
                Matcher m = AI_RESULT_PATTERN.matcher(content);
                if (m.find()) {
                    String desc = m.group(1).trim();
                    String stStr = m.group(2).trim();
                    desc = desc.replace("分析描述（字数不大于200，分析结果涉及的所有数值必须带上）", "").trim();
                    if (desc.endsWith("，") || desc.endsWith(",")) desc = desc.substring(0, desc.length() - 1);
                    result.description = desc;
                    if (stStr.contains("异常") && !stStr.contains("无异常")) {
                        result.status = 1;
                    }
                } else {
                    result.description = content;
                }
            }
        } catch (Exception e) {
            log.error("JSON 解析异常", e);
        }
        return result;
    }

    private String buildPrompt(String json, String timeCn) {
        return String.format(
                "%s\n" +
                        "你只允许基于输入 JSON 进行分析，禁止任何推断、补全、改写或重构数据。\n" +
                        "不要返回思考过程，只输出结论性内容。\n" +

                        "【设定时间点】%s\n" +

                        "【数据说明】\n" +
                        "- 「历史数据」：最近多日【相同时间点】的人流量，仅用于条件2；\n" +
                        "- 「当日数据」：当天 0 点至设定时间点的连续小时数据，仅用于条件1。\n" +

                        "【时间字段硬约束（必须严格执行）】\n" +
                        "- 所有输出中的「时间点」字符串，必须逐字等于输入 JSON 中对应记录的时间点；\n" +
                        "- 禁止对时间进行推算、替换、补全、重排或跨年映射；\n" +
                        "- 禁止将历史样本理解为“往年同一天”；\n" +
                        "- 若无法逐字复用输入时间点，条件2直接判定为“无异常”。\n" +

                        "【条件1｜当日环比】\n" +
                        "- 使用「当日数据」；\n" +
                        "- 设定时间点对应当日序号 = N；前1小时 = N-1；\n" +
                        "- 增幅 = (当前值 - 前1小时值) / 前1小时值 × 100%%；\n" +
                        "- 增幅 >30%% ⇒ 异常，否则 ⇒ 无异常；\n" +
                        "- 若前1小时不存在 ⇒ 条件1 = 无法判断。\n" +

                        "【条件2｜历史同时间对比】\n" +
                        "- 使用「历史数据」，禁止裁剪、跳过或合并样本；\n" +
                        "- 样本总数 = 历史数据数组长度；\n" +
                        "- 对每一条历史样本，逐条列出：\n" +
                        "  历史序号｜时间点（原文）｜历史值｜增幅%%｜是否>30%%（是/否）；\n" +
                        "- 超过60%%的样本增幅 >30%% ⇒ 条件2 = 异常，否则 = 无异常。\n" +

                        "【最终判定】\n" +
                        "- 仅当【条件1 = 异常 且 条件2 = 异常】⇒ 最终结果 = 异常；\n" +
                        "- 其余所有情况（含无法判断）⇒ 最终结果 = 无异常。\n" +

                        "【输出格式（必须严格遵守）】\n" +
                        "@1.分析描述（≤200字，含条件1与条件2逐样本说明）\n" +
                        "@2.最终结果（仅允许：异常 / 无异常）",
                json, timeCn
        );
    }




    private String callQwenAI(String prompt) {
        try {
            RestTemplate rt = new RestTemplate();

            JSONObject body = new JSONObject();
            body.put("model", "qwen3:32b-q4_K_M");

            JSONArray msgs = new JSONArray();

            JSONObject sys = new JSONObject();
            sys.put("role", "system");
            sys.put("content", "你是一个严格按规则执行的分析程序，只能基于输入数据计算结果。不要返回思考过程。");

            JSONObject usr = new JSONObject();
            usr.put("role", "user");
            usr.put("content", prompt);

            msgs.add(sys);
            msgs.add(usr);
            body.put("messages", msgs);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);

            return rt.postForObject(
                    "http://qwen.onemap.sh.cegn.cn/chat",
                    entity,
                    String.class
            );
        } catch (Exception e) {
            log.error("AI 接口请求失败", e);
            return null;
        }
    }


    private long toTs(LocalDateTime t) {
        return t.atZone(ZoneId.systemDefault()).toEpochSecond();
    }

    private static class AiResult {
        String description;
        int status;
    }

}