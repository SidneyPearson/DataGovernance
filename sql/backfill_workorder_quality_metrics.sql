-- 回填 dws_grid_workorder_daily 质量指标字段
-- 来源表：zjw_wgh_t_contactinfo_23
-- 口径：
--   label_fill_count = infotypename 非空
--   label_code_count = isstandard = '是' 或 '1'
--   grid_in_count    = gridcode 非空
--   focus_count      = isoverdue/isdelay = '是' 或 '1'
-- 说明：rate 分母沿用 dws_grid_workorder_daily.total_count（即该日期/区域汇集工单总量）

-- 1. 回填各区每日质量指标
UPDATE dws_grid_workorder_daily d
SET label_fill_count = q.label_fill_count,
    label_fill_rate  = CASE WHEN COALESCE(d.total_count, 0) = 0 THEN 0
                            ELSE LEAST(100, ROUND(q.label_fill_count * 100.0 / d.total_count, 4)) END,
    label_code_count = q.label_code_count,
    label_code_rate  = CASE WHEN COALESCE(d.total_count, 0) = 0 THEN 0
                            ELSE LEAST(100, ROUND(q.label_code_count * 100.0 / d.total_count, 4)) END,
    grid_in_count    = q.grid_in_count,
    grid_in_rate     = CASE WHEN COALESCE(d.total_count, 0) = 0 THEN 0
                            ELSE LEAST(100, ROUND(q.grid_in_count * 100.0 / d.total_count, 4)) END,
    focus_count      = q.focus_count,
    update_time      = CURRENT_TIMESTAMP
FROM (
    SELECT DATE(discovertime) AS stat_date,
           areacode AS area_code,
           COALESCE(SUM(CASE WHEN infotypename IS NOT NULL AND infotypename <> '' THEN 1 ELSE 0 END), 0) AS label_fill_count,
           COALESCE(SUM(CASE WHEN isstandard = '是' OR isstandard = '1' THEN 1 ELSE 0 END), 0) AS label_code_count,
           COALESCE(SUM(CASE WHEN gridcode IS NOT NULL AND gridcode <> '' THEN 1 ELSE 0 END), 0) AS grid_in_count,
           COALESCE(SUM(CASE WHEN isoverdue = '是' OR isoverdue = '1' OR isdelay = '是' OR isdelay = '1' THEN 1 ELSE 0 END), 0) AS focus_count
    FROM zjw_wgh_t_contactinfo_23
    WHERE discovertime IS NOT NULL
      AND areacode IS NOT NULL
      AND areacode <> ''
    GROUP BY DATE(discovertime), areacode
) q
WHERE d.area_code = q.area_code
  AND DATE(d.stat_date) = q.stat_date
  AND LOWER(d.area_code) <> 'city';

-- 2. 回填 CITY 每日质量指标
UPDATE dws_grid_workorder_daily d
SET label_fill_count = q.label_fill_count,
    label_fill_rate  = CASE WHEN COALESCE(d.total_count, 0) = 0 THEN 0
                            ELSE LEAST(100, ROUND(q.label_fill_count * 100.0 / d.total_count, 4)) END,
    label_code_count = q.label_code_count,
    label_code_rate  = CASE WHEN COALESCE(d.total_count, 0) = 0 THEN 0
                            ELSE LEAST(100, ROUND(q.label_code_count * 100.0 / d.total_count, 4)) END,
    grid_in_count    = q.grid_in_count,
    grid_in_rate     = CASE WHEN COALESCE(d.total_count, 0) = 0 THEN 0
                            ELSE LEAST(100, ROUND(q.grid_in_count * 100.0 / d.total_count, 4)) END,
    focus_count      = q.focus_count,
    update_time      = CURRENT_TIMESTAMP
FROM (
    SELECT DATE(discovertime) AS stat_date,
           COALESCE(SUM(CASE WHEN infotypename IS NOT NULL AND infotypename <> '' THEN 1 ELSE 0 END), 0) AS label_fill_count,
           COALESCE(SUM(CASE WHEN isstandard = '是' OR isstandard = '1' THEN 1 ELSE 0 END), 0) AS label_code_count,
           COALESCE(SUM(CASE WHEN gridcode IS NOT NULL AND gridcode <> '' THEN 1 ELSE 0 END), 0) AS grid_in_count,
           COALESCE(SUM(CASE WHEN isoverdue = '是' OR isoverdue = '1' OR isdelay = '是' OR isdelay = '1' THEN 1 ELSE 0 END), 0) AS focus_count
    FROM zjw_wgh_t_contactinfo_23
    WHERE discovertime IS NOT NULL
    GROUP BY DATE(discovertime)
) q
WHERE LOWER(d.area_code) = 'city'
  AND DATE(d.stat_date) = q.stat_date;

-- 3. 校验：查看回填后的 CITY 指标
SELECT to_char(stat_date, 'YYYY-MM-DD') AS stat_date,
       area_code,
       total_count,
       label_fill_count,
       label_fill_rate,
       label_code_count,
       label_code_rate,
       grid_in_count,
       grid_in_rate,
       focus_count
FROM dws_grid_workorder_daily
WHERE LOWER(area_code) = 'city'
ORDER BY stat_date DESC
LIMIT 10;
