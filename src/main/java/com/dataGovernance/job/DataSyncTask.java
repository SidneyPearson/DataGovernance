package com.dataGovernance.job;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.dataGovernance.domain.entity.DwdRlRecord;
import com.dataGovernance.domain.entity.GridConfig;
import com.dataGovernance.helper.JdbcBatchInserter;
import com.dataGovernance.utils.AISessionUtil;
import com.dataGovernance.utils.DBConnectionUtils;
import com.dataGovernance.utils.TokenUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
    private final TokenUtil tokenUtil;
    private final AISessionUtil aiSessionUtil;

    @Value("${ai.application_id}")
    private String aiApplicationId;
    @Value("${ai.project_id}")
    private String aiProjectId;
    @Value("${ai.url}")
    private String aiUrl;


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

        // 计算时间窗口 (例：当前小时 14:10 -> 抓取 13:00-14:00)
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

            // 1.获取AI接口的accessToken
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String token = tokenUtil.getAccessToken();
            headers.add("Authorization", "Bearer " + token);

            // 2.创建会话
            String conversationId = aiSessionUtil.createSession(headers);
            // 循环分析每个网格
            for (GridConfig grid : TARGET_GRIDS) {
                try {
                    analyzeSingleGrid(conn, grid, targetTime, targetTs, headers, conversationId);
                } catch (Exception e) {
                    log.error("网格 [{}] 分析失败", grid.getGridName(), e);
                }
            }

            // 3.停止会话
            aiSessionUtil.stopSession(headers, conversationId);


        } catch (Exception e) {
            log.error("[智能分析] 数据库连接异常", e);
        }

        log.info(">>>>>> [智能分析] 任务结束，耗时 {} ms <<<<<<", System.currentTimeMillis() - start);
    }

    // ==========================================
    // 私有方法：同步单个网格逻辑 (保持不变)
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
    public void analyzeSingleGrid(Connection conn, GridConfig grid, LocalDateTime targetTime,
                                  long targetTs, HttpHeaders headers, String conversationId) {
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
        log.info("历史数据：{}", historyJson);

        // 3. 组装 Prompt
        String timeCn = targetTime.format(FMT_CN_MD_H);
        String prompt = buildPrompt(historyJson, timeCn);

        // 4. 调用 AI (此处已修改为调用新接口)
        // 原始调用：String aiRawResp = callLegacyQwenAI(prompt);
        String aiRawResp = callNewQwenAI(prompt, headers, conversationId);

        log.info("Qwen响应结果：{}", aiRawResp);

        // 5. 解析结果并入库 (使用适配新接口的解析逻辑)
        saveWarningResult(conn, targetRecord, aiRawResp);
    }

    // ================== 数据库与工具方法 (已升级支持多网格) ==================

    private void saveWarningResult(Connection conn, DwdRlRecord record, String aiRawResp) {
        // 使用新解析方法
        AiResult result = parseNewAiResponse(aiRawResp);
        // 如果新解析失败（description为AI解析失败），可以考虑尝试旧解析，或者直接记录失败
        // 为了兼容性，如果新格式解析不到，尝试用旧格式解析一次(针对fallback情况)
        if (result.status == 0 && "AI解析失败".equals(result.description)) {
            AiResult legacyResult = parseLegacyAiResponse(aiRawResp);
            if (!"AI解析失败".equals(legacyResult.description)) {
                result = legacyResult;
            }
        }

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

        // 【新增】设定时间点 (格式保持和内部数据一致，如: 2026-01-06 14:00:00)
        root.put("设定时间点", targetTime.format(FMT_YMD_HMS));

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

        // 如果完全查不到数据，返回 null 以便上层跳过分析
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

    // ================== 新增：新的AI接口调用方法 ==================
    private String callNewQwenAI(String prompt, HttpHeaders headers, String conversationId) {
        try {
            RestTemplate rt = new RestTemplate();

            JSONObject body = new JSONObject();
            body.put("text", prompt);
            body.put("files", new JSONArray());
            body.put("project_id", aiProjectId);
            body.put("stream", false);
            body.put("using_context", false);
            body.put("application_id", aiApplicationId);
            body.put("conversation_id", conversationId);
            body.put("model_name", "Qwen3-32B");
            body.put("model_params", new JSONObject());
            body.put("is_temp", 1);
            body.put("version", 2);
            body.put("max_tokens", 2048);

            HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);

            String url = aiUrl + "/api/ai/chat?secret_key=5e1fdb5bc1374cd48373ba44c83d8919";

            return rt.postForObject(url, entity, String.class);

        } catch (Exception e) {
            log.error("AI 新接口请求失败", e);
            return null;
        }
    }

    // ================== 新增：新接口结果解析方法 ==================
    private AiResult parseNewAiResponse(String jsonResp) {
        AiResult result = new AiResult();
        result.description = "AI解析失败";
        result.status = 0;
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonResp);

            // 新结构： data -> answer
            JsonNode dataNode = root.path("data");
            if (!dataNode.isMissingNode() && dataNode.has("answer")) {
                String content = dataNode.path("answer").asText();
                log.info("AI返回结果(New)：" + content);

                // 复用原有的 Regex 逻辑处理内容
                processContent(content, result);
            }
        } catch (Exception e) {
            log.error("JSON 解析异常 (New)", e);
        }
        return result;
    }

    // ================== 保留：旧的AI接口调用方法 (改名保留) ==================
    private String callLegacyQwenAI(String prompt) {
        try {
            RestTemplate rt = new RestTemplate();

            JSONObject body = new JSONObject();
            body.put("model", "qwen3:32b-q4_K_M");

            JSONArray msgs = new JSONArray();

            JSONObject sys = new JSONObject();
            sys.put("role", "system");
            sys.put("content",
                    "你是一个规则驱动的计算程序，不具备主观判断能力，" +
                            "只能按指令逐条计算并输出结果。"
            );

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
            log.error("AI 旧接口请求失败", e);
            return null;
        }
    }

    // ================== 保留：旧的解析逻辑 (用于解析旧接口响应) ==================
    private AiResult parseLegacyAiResponse(String jsonResp) {
        AiResult result = new AiResult();
        result.description = "AI解析失败";
        result.status = 0;
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonResp);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                String content = choices.get(0).path("message").path("content").asText();
                log.info("AI返回结果(Legacy)：" + content);
                processContent(content, result);
            }
        } catch (Exception e) {
            log.error("JSON 解析异常 (Legacy)", e);
        }
        return result;
    }

    // ================== 抽取：通用的文本处理逻辑 ==================
    private void processContent(String content, AiResult result) {
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

    private String buildPrompt(String json, String timeCn) {
//        return String.format(
//                "%s\n" +
//
//                        "【最高优先级指令｜必须遵守】\n" +
//                        "- 严禁返回 <think>、推理过程、思考步骤、内部分析\n" +
//                        "- 严禁解释你是如何得出结论的\n" +
//                        "- 只能按【输出格式】返回最终文本\n" +
//                        "- 若无法严格遵守规则，视为输出错误\n" +
//
//                        "【分析设定时间点】%s\n" +
//
//                        "【数据使用总规则】\n" +
//                        "- 你只能基于输入 JSON 中已存在的数据进行计算\n" +
//                        "- 禁止推断、补全、重排、改写、映射或重构任何数据\n" +
//                        "- 禁止自行假设缺失值、默认值或业务含义\n" +
//
//                        "【数据结构说明】\n" +
//                        "1. 当日数据：当天 00:00 → 设定时间点的连续小时数据，仅用于条件1\n" +
//                        "2. 历史数据：最近多日【相同时间点】的数据，仅用于条件2\n" +
//
//                        "【时间字段硬约束（极其重要）】\n" +
//                        "- 所有输出中的“时间点”必须逐字等于输入 JSON 中对应记录的时间点\n" +
//                        "- 禁止对时间进行推算、替换、补全、跨日或跨年映射\n" +
//                        "- 禁止将历史数据理解为“往年同一天”\n" +
//                        "- 若无法逐字复用输入时间点字符串，则条件2直接判定为【无异常】\n" +
//
//                        "【条件1｜当日环比判定】\n" +
//                        "- 仅允许使用【当日数据】\n" +
//                        "- 设定时间点对应当日序号 = N，前1小时 = N-1\n" +
//                        "- 增幅 = (当前值 - 前1小时值) / 前1小时值 × 100%%\n" +
//                        "- 增幅 > 30%% ⇒ 条件1 = 异常，否则 ⇒ 条件1 = 无异常\n" +
//                        "- 若前1小时不存在或无法计算 ⇒ 条件1 = 无法判断\n" +
//
//                        "【条件2｜历史同时间对比判定】\n" +
//                        "- 仅允许使用【历史数据】\n" +
//                        "- 禁止裁剪、跳过、合并或忽略任何历史样本\n" +
//                        "- 历史样本总数 = 历史数据数组的实际长度\n" +
//                        "- 必须对每一条历史样本逐条、显式列出以下信息：\n" +
//                        "  历史序号｜时间点（原文）｜历史值｜当前值｜增幅%%｜是否>30%%（是/否）\n" +
//                        "- 禁止使用“分别为”“依次为”等概括性描述\n" +
//                        "- 增幅 >30%% 的样本数 / 总样本数 > 60%% ⇒ 条件2 = 异常，否则 = 无异常\n" +
//
//                        "【最终结果硬性规则】\n" +
//                        "- 仅当【条件1 = 异常 且 条件2 = 异常】⇒ 最终结果 = 异常\n" +
//                        "- 其余所有情况（含 无异常 / 无法判断）⇒ 最终结果 = 无异常\n" +
//                        "- 禁止分析描述与最终结果不一致\n" +
//                        "- 若出现不一致，直接视为输出错误\n" +
//
//                        "【唯一允许的输出格式】\n" +
//                        "@1.分析描述\n" +
//                        "@2.最终结果（仅允许：异常 / 无异常）",
//                json, timeCn
//        );
        return json;
    }

    private long toTs(LocalDateTime t) {
        return t.atZone(ZoneId.systemDefault()).toEpochSecond();
    }

    private static class AiResult {
        String description;
        int status;
    }

}