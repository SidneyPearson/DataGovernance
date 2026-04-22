package com.dataGovernance.service;

import com.dataGovernance.domain.entity.GridConfig;
import com.dataGovernance.job.DataSyncTask;
import com.dataGovernance.utils.AISessionUtil;
import com.dataGovernance.utils.DBConnectionUtils;
import com.dataGovernance.utils.TokenUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DataAnalyzeService {

    private final DataSyncTask dataSyncTask;

    private final TokenUtil tokenUtil;

    private final AISessionUtil aiSessionUtil;

    private static final Logger log = LoggerFactory.getLogger(DataAnalyzeService.class);

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final List<GridConfig> TARGET_GRIDS = Arrays.asList(
            new GridConfig("4151795597343053249", "ZH31011590201", "国际旅游度假区综合网格", "上海国际旅游度假区", "浦东新区"),
            new GridConfig("4151772507108546469", "ZH31010101301", "第一综合网格", "外滩街道", "黄浦区"),
            new GridConfig("4151772519818439747", "ZH31010101302", "第二综合网格", "外滩街道", "黄浦区"),
            new GridConfig("4151772520350577084", "ZH31010101303", "第三综合网格", "外滩街道", "黄浦区")
    );

    public void analyzeRange(String start, String end, List<String> gridCodes) {

        LocalDateTime startTime = LocalDateTime.parse(start, FMT);
        LocalDateTime endTime = LocalDateTime.parse(end, FMT);

        try (Connection conn = DBConnectionUtils.getDMConnection(
                "skyt_slxx_dghy_prod",
                "Skyt_slxx_dghy_prod@2025",
                "skyt_slxx_dghy_prod")) {

            // 选择网格
            List<GridConfig> grids = filterGrids(gridCodes);

            // 1.获取AI接口的accessToken
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String token = tokenUtil.getAccessToken();
            headers.add("Authorization", "Bearer " + token);

            // 2.创建会话
            String conversationId = aiSessionUtil.createSession(headers);

            LocalDateTime cursor = startTime;
            while (!cursor.isAfter(endTime)) {

                long ts = cursor.atZone(ZoneId.systemDefault()).toEpochSecond();

                for (GridConfig grid : grids) {
                    try {
                        dataSyncTask.analyzeSingleGrid(
                                conn,
                                grid,
                                cursor,
                                ts,
                                headers,
                                conversationId
                        );
                    } catch (Exception e) {
                        log.error("分析失败 grid={} time={}",
                                grid.getGridName(), cursor, e);
                    }
                }

                cursor = cursor.plusHours(1);
            }

            // 3.停止会话
            aiSessionUtil.stopSession(headers, conversationId);

        } catch (Exception e) {
            throw new RuntimeException("分析区间任务失败", e);
        }
    }

    private List<GridConfig> filterGrids(List<String> gridCodes) {
        if (gridCodes == null || gridCodes.isEmpty()) {
            return TARGET_GRIDS;
        }
        return TARGET_GRIDS.stream()
                .filter(g -> gridCodes.contains(g.getGridCode()))
                .collect(Collectors.toList());
    }
}
