-- ============================================================
-- 数智网格数据运营监控 - DDL 初始化脚本
-- 数据库：KingbaseES  schema：dsjzx
-- ============================================================

SET search_path = dsjzx;

-- ------------------------------------------------------------
-- 1. 数据级联事实表 dws_grid_cascade_log
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dsjzx.dws_grid_cascade_log (
    id              bigserial       NOT NULL,
    task_code       varchar(100)    NOT NULL,
    task_name       varchar(200),
    task_type       varchar(20),
    dim_code        varchar(100),
    dim_name        varchar(200),
    frequency       varchar(50),
    schedule_rule   varchar(200),
    cascade_count   bigint          DEFAULT 0,
    cascade_status  varchar(20)     DEFAULT 'SUCCESS',
    start_time      timestamp,
    end_time        timestamp,
    duration_ms     bigint,
    error_msg       text,
    stat_date       date,
    create_time     timestamp       DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_dws_grid_cascade_log PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_cascade_log_stat_date  ON dsjzx.dws_grid_cascade_log (stat_date);
CREATE INDEX IF NOT EXISTS idx_cascade_log_task_code  ON dsjzx.dws_grid_cascade_log (task_code);
CREATE INDEX IF NOT EXISTS idx_cascade_log_task_type  ON dsjzx.dws_grid_cascade_log (task_type);

-- ------------------------------------------------------------
-- 2. 工单日指标汇总表 dws_grid_workorder_daily
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dsjzx.dws_grid_workorder_daily (
    id                  bigserial       NOT NULL,
    stat_date           date            NOT NULL,
    area_code           varchar(50)     NOT NULL,
    area_name           varchar(100),
    total_count         bigint          DEFAULT 0,
    src_12345_count     bigint          DEFAULT 0,
    src_zjb_count       bigint          DEFAULT 0,
    new_count           bigint          DEFAULT 0,
    label_fill_count    bigint          DEFAULT 0,
    label_fill_rate     numeric(7,4)    DEFAULT 0,
    label_code_count    bigint          DEFAULT 0,
    label_code_rate     numeric(7,4)    DEFAULT 0,
    grid_in_count       bigint          DEFAULT 0,
    grid_in_rate        numeric(7,4)    DEFAULT 0,
    focus_count         bigint          DEFAULT 0,
    city_ratio          numeric(7,4)    DEFAULT 0,
    change_count        bigint          DEFAULT 0,
    change_trend        varchar(10),
    collect_status      varchar(20)     DEFAULT 'DONE',
    collect_time        timestamp,
    update_status       varchar(20)     DEFAULT 'UPDATED',
    update_time         timestamp,
    create_time         timestamp       DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_dws_grid_workorder_daily PRIMARY KEY (id),
    CONSTRAINT uq_workorder_daily_date_area UNIQUE (stat_date, area_code)
);

CREATE INDEX IF NOT EXISTS idx_workorder_daily_stat_date ON dsjzx.dws_grid_workorder_daily (stat_date);
CREATE INDEX IF NOT EXISTS idx_workorder_daily_area_code ON dsjzx.dws_grid_workorder_daily (area_code);
CREATE INDEX IF NOT EXISTS idx_workorder_daily_date_area ON dsjzx.dws_grid_workorder_daily (stat_date, area_code);

-- ------------------------------------------------------------
-- 3. 工单备注表 ads_grid_workorder_remark
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dsjzx.ads_grid_workorder_remark (
    id              bigserial       NOT NULL,
    task_id         varchar(200)    NOT NULL,
    src_type        varchar(20),
    target_type     varchar(50),
    target_desc     varchar(500),
    remark_content  text            NOT NULL,
    stat_date       date,
    create_by       varchar(100),
    create_time     timestamp       DEFAULT CURRENT_TIMESTAMP,
    update_by       varchar(100),
    update_time     timestamp,
    del_flag        smallint        DEFAULT 0,
    CONSTRAINT pk_ads_grid_workorder_remark PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_remark_task_id   ON dsjzx.ads_grid_workorder_remark (task_id);
CREATE INDEX IF NOT EXISTS idx_remark_stat_date ON dsjzx.ads_grid_workorder_remark (stat_date);
CREATE INDEX IF NOT EXISTS idx_remark_del_flag  ON dsjzx.ads_grid_workorder_remark (del_flag);