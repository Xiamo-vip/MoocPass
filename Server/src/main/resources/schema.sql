-- 用户表结构定义示例
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    `password` VARCHAR(100) NOT NULL COMMENT '密码 (加密)',
    `email` VARCHAR(100) DEFAULT NULL UNIQUE COMMENT '邮箱',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 用户网课平台账号/设置表
CREATE TABLE IF NOT EXISTS `user_platform_config` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '所属系统用户ID',
    `platform_code` VARCHAR(32) NOT NULL COMMENT '平台代码（如 chaoxing, zhy）',
    `username` VARCHAR(100) DEFAULT NULL COMMENT '平台账号/手机号/显示名',
    `password` VARCHAR(255) DEFAULT NULL COMMENT '平台密码',
    `token` TEXT DEFAULT NULL COMMENT 'OAuth 认证凭证 Token',
    `cookies` TEXT DEFAULT NULL COMMENT '登录 Cookie 缓存',
    `status` INT DEFAULT 1 COMMENT '状态：1 正常，0 异常/未绑定',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_user_platform` (`user_id`, `platform_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户网课平台账号设置表';


-- 用户题库与AI配置表
CREATE TABLE IF NOT EXISTS `user_question_config` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `user_id` BIGINT NOT NULL UNIQUE COMMENT '所属系统用户ID',
    `provider` VARCHAR(50) DEFAULT 'AI' COMMENT '默认题库类型：AI 等',
    `ai_base_url` VARCHAR(255) DEFAULT NULL COMMENT 'AI 接口 Base URL',
    `ai_key` VARCHAR(255) DEFAULT NULL COMMENT 'AI API Key',
    `ai_model` VARCHAR(100) DEFAULT NULL COMMENT 'AI 模型名称',
    `submit` TINYINT(1) DEFAULT 1 COMMENT '是否提交答题',
    `cover_rate` DOUBLE DEFAULT 0.9 COMMENT '最低题库覆盖率',
    `min_interval_seconds` INT DEFAULT 3 COMMENT '请求最小间隔秒数',
    `config_json` TEXT DEFAULT NULL COMMENT '各题库/解题器独立配置 JSON',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户题库与AI配置表';

-- 刷课任务表
CREATE TABLE IF NOT EXISTS `course_task` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '所属系统用户ID',
    `platform_code` VARCHAR(32) NOT NULL COMMENT '平台代码',
    `course_id` VARCHAR(64) NOT NULL COMMENT '网课平台课程ID',
    `class_id` VARCHAR(64) DEFAULT NULL COMMENT '网课平台班级ID',
    `course_name` VARCHAR(255) NOT NULL COMMENT '课程名称',
    `cover_url` VARCHAR(500) DEFAULT NULL COMMENT '课程封面图片URL',
    `teacher` VARCHAR(100) DEFAULT NULL COMMENT '任课老师',
    `speed` DOUBLE DEFAULT 1.0 COMMENT '播放倍速',
    `question_provider` VARCHAR(50) DEFAULT 'AI' COMMENT '所选题库策略',
    `auto_answer` TINYINT(1) DEFAULT 1 COMMENT '是否进行答题',
    `submit_answer` TINYINT(1) DEFAULT 1 COMMENT '答题处理模式：1 提交，0 仅保存',
    `cover_rate` DOUBLE DEFAULT 0.8 COMMENT '最小答题覆盖率',
    `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT '任务状态：PENDING, RUNNING, COMPLETED, FAILED, STOPPED',
    `progress` INT DEFAULT 0 COMMENT '完成进度 (0-100)',
    `current_chapter` VARCHAR(255) DEFAULT NULL COMMENT '当前处理章节',
    `error_message` TEXT DEFAULT NULL COMMENT '错误说明信息',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='刷课任务表';

-- 刷课任务持久化全量日志表 (一任务一记录)
CREATE TABLE IF NOT EXISTS `course_task_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `task_id` BIGINT NOT NULL UNIQUE COMMENT '关联的刷课任务ID (唯一)',
    `full_log` LONGTEXT DEFAULT NULL COMMENT '全量运行日志文本',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_task_id` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='刷课任务持久化全量日志表';

-- 题库客观题缓存表 (选择题/填空题缓存，主观大题不缓存)
CREATE TABLE IF NOT EXISTS `question_cache` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `question_hash` VARCHAR(64) NOT NULL UNIQUE COMMENT '题目文本 MD5 哈希',
    `question` TEXT NOT NULL COMMENT '题目原文本',
    `question_type` VARCHAR(30) NOT NULL COMMENT '题目类型: single, multiple, completion, judgement等',
    `answer` TEXT NOT NULL COMMENT '答案内容',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_question_hash` (`question_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题库客观题缓存表';


