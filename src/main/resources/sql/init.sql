-- =================================================================
-- 【第一章讲解】init.sql — 多租户 SaaS 销售 CRM 系统初始化脚本
-- =================================================================
-- SQL 基础知识说明：
--   CREATE DATABASE = 创建数据库（相当于建一个"文件夹"）
--   CREATE TABLE    = 创建数据表（相当于建一个"Excel 表格"）
--   INSERT INTO     = 插入数据（往表格里加一行数据）
--   PRIMARY KEY     = 主键，每行数据的唯一标识（就像身份证号）
--   NOT NULL        = 该列不允许为空
--   DEFAULT         = 没填时自动使用的默认值
--   COMMENT         = 字段说明（方便自己和同事理解字段含义）
-- =================================================================

-- 【Step 1】创建数据库（如果已存在就不覆盖）
CREATE DATABASE IF NOT EXISTS saas_crm
    DEFAULT CHARACTER SET utf8mb4   -- utf8mb4 支持 Emoji 等特殊字符，比 utf8 更完整
    DEFAULT COLLATE utf8mb4_unicode_ci;

-- 切换到这个数据库
USE saas_crm;


-- =================================================================
-- 【表1】租户表 tenant
-- 数据隔离的核心概念：每个"公司"就是一个租户
-- 例如：字节跳动 是租户1，阿里巴巴 是租户2，它们的数据绝对不能互相看到
-- =================================================================
CREATE TABLE IF NOT EXISTS `tenant` (
    `id`          BIGINT       NOT NULL COMMENT '主键 ID（雪花算法生成，全局唯一）',
    `name`        VARCHAR(100) NOT NULL COMMENT '公司名称',
    `code`        VARCHAR(50)  NOT NULL COMMENT '租户编码（登录时区分用，如 bytedance）',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1=正常，0=已禁用',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=正常，1=已删除',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)    -- code 不能重复，加唯一索引
) COMMENT='租户表（每个租户代表一家公司）';


-- =================================================================
-- 【表2】角色表 sys_role
-- RBAC（基于角色的访问控制）的核心：先定义有哪些角色
-- 这张表不需要 tenant_id，因为角色类型是全局通用的
-- =================================================================
CREATE TABLE IF NOT EXISTS `sys_role` (
    `id`          BIGINT      NOT NULL COMMENT '主键 ID',
    `code`        VARCHAR(50) NOT NULL COMMENT '角色编码（程序里用这个判断权限）',
    `name`        VARCHAR(50) NOT NULL COMMENT '角色名称（显示给用户看）',
    `data_scope`  TINYINT     NOT NULL DEFAULT 4 COMMENT '数据范围：1=全部 2=本租户 3=本部门 4=仅本人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) COMMENT='系统角色表';

-- 【重点】数据范围 data_scope 的含义（这就是数据权限的核心！）
-- data_scope = 1：SUPER_ADMIN  超级管理员，能看所有租户的所有数据
-- data_scope = 2：TENANT_ADMIN 租户管理员，只能看本公司数据
-- data_scope = 3：DEPT_MANAGER 部门经理，  只能看本部门数据
-- data_scope = 4：SALESPERSON  销售员，    只能看自己负责的数据


-- =================================================================
-- 【表3】部门表 dept
-- 注意：有 tenant_id 字段！不同公司的部门互不可见
-- =================================================================
CREATE TABLE IF NOT EXISTS `dept` (
    `id`          BIGINT       NOT NULL COMMENT '主键 ID',
    `tenant_id`   BIGINT       NOT NULL COMMENT '所属租户 ID（关联 tenant 表）',
    `name`        VARCHAR(100) NOT NULL COMMENT '部门名称',
    `parent_id`   BIGINT                COMMENT '父部门 ID（为 NULL 表示顶级部门）',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`)
) COMMENT='部门表';


-- =================================================================
-- 【表4】用户表 sys_user
-- 每个用户：属于某个租户 + 属于某个部门 + 拥有某个角色
-- 这三个信息共同决定了"他能看哪些数据"
-- =================================================================
CREATE TABLE IF NOT EXISTS `sys_user` (
    `id`          BIGINT       NOT NULL COMMENT '主键 ID',
    `tenant_id`   BIGINT       NOT NULL COMMENT '所属租户 ID',
    `dept_id`     BIGINT                COMMENT '所属部门 ID',
    `role_id`     BIGINT       NOT NULL COMMENT '角色 ID（关联 sys_role 表）',
    `username`    VARCHAR(50)  NOT NULL COMMENT '登录用户名',
    `password`    VARCHAR(100) NOT NULL COMMENT '密码（实际项目中存加密后的值）',
    `real_name`   VARCHAR(50)           COMMENT '真实姓名',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) COMMENT='系统用户表';


-- =================================================================
-- 【表5】客户表 customer（核心业务表）
-- 这张表通过 tenant_id + owner_id + dept_id 三个字段
-- 实现了三层数据权限过滤：租户隔离 / 部门过滤 / 个人过滤
-- =================================================================
CREATE TABLE IF NOT EXISTS `customer` (
    `id`          BIGINT       NOT NULL COMMENT '主键 ID',
    `tenant_id`   BIGINT       NOT NULL COMMENT '所属租户（数据隔离的核心字段）',
    `dept_id`     BIGINT       NOT NULL COMMENT '所属部门（部门经理权限过滤用）',
    `owner_id`    BIGINT       NOT NULL COMMENT '负责销售的用户 ID（个人权限过滤用）',
    `name`        VARCHAR(100) NOT NULL COMMENT '客户名称（公司名 或 个人名）',
    `phone`       VARCHAR(20)           COMMENT '联系电话',
    `industry`    VARCHAR(50)           COMMENT '所在行业',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '客户状态：1=跟进中，2=已成交，3=已流失',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    -- 添加索引加速查询（WHERE tenant_id=? 和 WHERE owner_id=? 都会变快）
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_owner_id` (`owner_id`),
    KEY `idx_dept_id` (`dept_id`)
) COMMENT='客户表';


-- =================================================================
-- 【表6】订单表 order_info
-- 订单属于客户，客户属于租户，所以订单也有 tenant_id
-- =================================================================
CREATE TABLE IF NOT EXISTS `order_info` (
    `id`           BIGINT         NOT NULL COMMENT '主键 ID',
    `tenant_id`    BIGINT         NOT NULL COMMENT '所属租户',
    `customer_id`  BIGINT         NOT NULL COMMENT '关联的客户 ID',
    `amount`       DECIMAL(12, 2) NOT NULL COMMENT '订单金额（精确到分，不用 float 防止精度丢失）',
    `status`       TINYINT        NOT NULL DEFAULT 1 COMMENT '订单状态：1=待付款，2=已完成，3=已取消',
    `deleted`      TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time`  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_customer_id` (`customer_id`)
) COMMENT='订单表';


-- =================================================================
-- 【初始化数据】INSERT 测试数据
-- 目的：让你登录不同账号后，能直观看到数据权限生效的效果
-- =================================================================

-- ---- 角色数据 ----
INSERT INTO `sys_role` (`id`, `code`, `name`, `data_scope`) VALUES
(1, 'SUPER_ADMIN',  '超级管理员', 1),   -- data_scope=1：看所有数据
(2, 'TENANT_ADMIN', '租户管理员', 2),   -- data_scope=2：看本租户所有数据
(3, 'DEPT_MANAGER', '部门经理',   3),   -- data_scope=3：看本部门数据
(4, 'SALESPERSON',  '销售员',     4);   -- data_scope=4：只看自己的数据

-- ---- 租户数据（2家公司）----
INSERT INTO `tenant` (`id`, `name`, `code`) VALUES
(1001, '星云科技有限公司', 'xingyun'),
(1002, '海浪电商集团',     'hailang');

-- ---- 部门数据 ----
INSERT INTO `dept` (`id`, `tenant_id`, `name`, `parent_id`) VALUES
-- 星云科技的部门
(101, 1001, '销售一部', NULL),
(102, 1001, '销售二部', NULL),
-- 海浪电商的部门
(201, 1002, '华北销售部', NULL),
(202, 1002, '华南销售部', NULL);

-- ---- 用户数据 ----
-- 密码都是 123456（实际项目中需要加密存储，这里为了演示简化）
INSERT INTO `sys_user` (`id`, `tenant_id`, `dept_id`, `role_id`, `username`, `password`, `real_name`) VALUES
-- 超级管理员（不属于任何租户，跨租户看数据）
(1, 0,    NULL, 1, 'superadmin', '123456', '平台超管'),
-- 星云科技的用户
(2, 1001, NULL, 2, 'admin_xy',   '123456', '星云-管理员'),   -- 租户管理员
(3, 1001, 101,  3, 'mgr_xy_1',   '123456', '星云-一部经理'), -- 部门经理
(4, 1001, 101,  4, 'sales_xy_1', '123456', '星云-销售员A'),  -- 销售员
(5, 1001, 101,  4, 'sales_xy_2', '123456', '星云-销售员B'),  -- 销售员
(6, 1001, 102,  4, 'sales_xy_3', '123456', '星云-销售员C'),  -- 属于二部的销售员
-- 海浪电商的用户
(7, 1002, NULL, 2, 'admin_hl',   '123456', '海浪-管理员'),
(8, 1002, 201,  4, 'sales_hl_1', '123456', '海浪-销售员X');

-- ---- 客户数据 ----
INSERT INTO `customer` (`id`, `tenant_id`, `dept_id`, `owner_id`, `name`, `phone`, `industry`, `status`) VALUES
-- 星云科技的客户（tenant_id=1001）
(1, 1001, 101, 4, '北京优品科技',   '13800001111', '互联网', 1),   -- 销售员A负责
(2, 1001, 101, 4, '上海新零售集团', '13800002222', '零售',   2),   -- 销售员A负责（已成交）
(3, 1001, 101, 5, '广州贸易公司',   '13800003333', '贸易',   1),   -- 销售员B负责
(4, 1001, 102, 6, '深圳制造厂',     '13800004444', '制造',   1),   -- 二部销售员C负责
-- 海浪电商的客户（tenant_id=1002）
(5, 1002, 201, 8, '杭州电商运营',   '13900005555', '电商',   1),
(6, 1002, 201, 8, '成都直播基地',   '13900006666', '传媒',   2);

-- ---- 订单数据 ----
INSERT INTO `order_info` (`id`, `tenant_id`, `customer_id`, `amount`, `status`) VALUES
(1, 1001, 1, 58000.00,  1),   -- 客户1的待付款订单
(2, 1001, 2, 120000.00, 2),   -- 客户2的已完成订单
(3, 1001, 3, 35000.00,  1),   -- 客户3的订单
(4, 1002, 5, 88000.00,  2),   -- 海浪电商的订单
(5, 1002, 6, 210000.00, 2);


-- =================================================================
-- 【验证】执行这些查询，确认数据已正确插入
-- =================================================================
SELECT '=== 租户列表 ===' AS info;
SELECT id, name, code FROM tenant;

SELECT '=== 用户列表（含角色和部门信息）===' AS info;
SELECT
    u.id,
    u.real_name,
    u.username,
    t.name AS tenant_name,
    d.name AS dept_name,
    r.name AS role_name,
    r.data_scope
FROM sys_user u
LEFT JOIN tenant t ON u.tenant_id = t.id
LEFT JOIN dept d ON u.dept_id = d.id
LEFT JOIN sys_role r ON u.role_id = r.id
ORDER BY u.id;

SELECT '=== 客户总数（按租户）===' AS info;
SELECT t.name AS tenant_name, COUNT(c.id) AS customer_count
FROM customer c
JOIN tenant t ON c.tenant_id = t.id
GROUP BY t.id, t.name;
