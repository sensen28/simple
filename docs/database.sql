-- 创建数据库
CREATE DATABASE IF NOT EXISTS imchat DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE imchat;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` bigint NOT NULL COMMENT '用户ID',
    `username` varchar(32) NOT NULL COMMENT '用户名',
    `password` varchar(255) NOT NULL COMMENT '密码',
    `avatar` varchar(255) DEFAULT '' COMMENT '头像',
    `nickname` varchar(32) DEFAULT '' COMMENT '昵称',
    `signature` varchar(255) DEFAULT '' COMMENT '个性签名',
    `status` tinyint DEFAULT 1 COMMENT '用户状态：0-离线，1-在线，2-隐身',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 好友关系表
CREATE TABLE IF NOT EXISTS `friend` (
    `id` bigint NOT NULL COMMENT '主键ID',
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `friend_id` bigint NOT NULL COMMENT '好友ID',
    `remark` varchar(32) DEFAULT '' COMMENT '好友备注',
    `status` tinyint DEFAULT 1 COMMENT '关系状态：0-拉黑，1-正常',
    `is_mute` tinyint DEFAULT 0 COMMENT '是否免打扰：0-提醒，1-免打扰',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_friend` (`user_id`, `friend_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_friend_id` (`friend_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友关系表';

-- 好友申请表
CREATE TABLE IF NOT EXISTS `friend_apply` (
    `id` bigint NOT NULL COMMENT '主键ID',
    `apply_user_id` bigint NOT NULL COMMENT '申请人ID',
    `target_user_id` bigint NOT NULL COMMENT '目标用户ID',
    `remark` varchar(255) DEFAULT '' COMMENT '申请备注',
    `status` tinyint DEFAULT 0 COMMENT '申请状态：0-待处理，1-同意，2-拒绝',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_target_user_id` (`target_user_id`),
    KEY `idx_apply_user_id` (`apply_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友申请表';

-- 消息表
CREATE TABLE IF NOT EXISTS `message` (
    `id` bigint NOT NULL COMMENT '消息ID',
    `from_user_id` bigint NOT NULL COMMENT '发送者ID',
    `to_user_id` bigint NOT NULL COMMENT '接收者ID',
    `content` text NOT NULL COMMENT '消息内容',
    `type` tinyint DEFAULT 1 COMMENT '消息类型：1-文本，2-图片，3-文件，4-语音，5-视频',
    `status` tinyint DEFAULT 0 COMMENT '消息状态：0-未读，1-已读，2-撤回',
    `is_encrypted` tinyint DEFAULT 1 COMMENT '是否加密：0-未加密，1-已加密',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_from_to` (`from_user_id`, `to_user_id`),
    KEY `idx_to_user_id` (`to_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';
