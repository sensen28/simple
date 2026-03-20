-- 演示账号，默认密码均为 Demo@123456，仅建议本地开发与联调用途使用。
INSERT INTO `user` (
    `id`,
    `username`,
    `password`,
    `avatar`,
    `nickname`,
    `signature`,
    `status`,
    `create_time`,
    `update_time`
) VALUES
    (
        10001,
        'demo_alice',
        '$2a$10$bvQUOq1Uam2vAQRglKJYDOc72ao.Ss.IC2fyfNEvLRPrheylbGYEW',
        '',
        'Demo Alice',
        'Flyway seeded demo account',
        0,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        10002,
        'demo_bob',
        '$2a$10$bvQUOq1Uam2vAQRglKJYDOc72ao.Ss.IC2fyfNEvLRPrheylbGYEW',
        '',
        'Demo Bob',
        'Flyway seeded demo account',
        0,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    )
AS new_users
ON DUPLICATE KEY UPDATE
    `password` = new_users.`password`,
    `avatar` = new_users.`avatar`,
    `nickname` = new_users.`nickname`,
    `signature` = new_users.`signature`,
    `status` = new_users.`status`,
    `update_time` = CURRENT_TIMESTAMP;
