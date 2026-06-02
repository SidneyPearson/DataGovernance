-- 按区回填 dws_grid_workorder_daily 质量指标字段
-- 来源表：zjw_wgh_t_contactinfo_23
-- 口径：每个区按 area_code 真实聚合，不按全市比例分配
-- 注意：rate 字段使用 LEAST(100, ...) 防止源表指标数大于 dws total_count 时 numeric 溢出

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
           area_code AS area_code,
           COALESCE(SUM(CASE WHEN infotypename IS NOT NULL AND infotypename <> '' THEN 1 ELSE 0 END), 0) AS label_fill_count,
           COALESCE(SUM(CASE WHEN isstandard = '是' OR isstandard = '1' THEN 1 ELSE 0 END), 0) AS label_code_count,
           COALESCE(SUM(CASE WHEN gridcode IS NOT NULL AND gridcode <> '' THEN 1 ELSE 0 END), 0) AS grid_in_count,
           COALESCE(SUM(CASE WHEN isoverdue = '是' OR isoverdue = '1' OR isdelay = '是' OR isdelay = '1' THEN 1 ELSE 0 END), 0) AS focus_count
    FROM zjw_wgh_t_contactinfo_23
    WHERE discovertime IS NOT NULL
      AND area_code IS NOT NULL
      AND area_code <> ''
    GROUP BY DATE(discovertime), area_code
) q
WHERE d.area_code = q.area_code
  AND DATE(d.stat_date) = q.stat_date
  AND LOWER(d.area_code) <> 'city';

-- 2. 回填 CITY 每日质量指标：CITY = 各区质量指标求和
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
    SELECT stat_date,
           SUM(label_fill_count) AS label_fill_count,
           SUM(label_code_count) AS label_code_count,
           SUM(grid_in_count) AS grid_in_count,
           SUM(focus_count) AS focus_count
    FROM dws_grid_workorder_daily
    WHERE LOWER(area_code) <> 'city'
    GROUP BY stat_date
) q
WHERE LOWER(d.area_code) = 'city'
  AND DATE(d.stat_date) = DATE(q.stat_date);

-- 3. 校验：各区合计与 CITY 对比
SELECT to_char(c.stat_date, 'YYYY-MM-DD') AS stat_date,
       c.total_count AS city_total,
       s.sum_total AS district_total,
       c.label_fill_count AS city_label_fill,
       s.sum_label_fill AS district_label_fill,
       c.label_code_count AS city_label_code,
       s.sum_label_code AS district_label_code,
       c.grid_in_count AS city_grid_in,
       s.sum_grid_in AS district_grid_in,
       c.focus_count AS city_focus,
       s.sum_focus AS district_focus
FROM dws_grid_workorder_daily c
JOIN (
    SELECT stat_date,
           SUM(total_count) AS sum_total,
           SUM(label_fill_count) AS sum_label_fill,
           SUM(label_code_count) AS sum_label_code,
           SUM(grid_in_count) AS sum_grid_in,
           SUM(focus_count) AS sum_focus
    FROM dws_grid_workorder_daily
    WHERE LOWER(area_code) <> 'city'
    GROUP BY stat_date
) s ON DATE(c.stat_date) = DATE(s.stat_date)
WHERE LOWER(c.area_code) = 'city'
ORDER BY c.stat_date DESC
LIMIT 10;
