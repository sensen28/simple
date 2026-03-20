# 联调测试清单

## 1. 启动顺序

1. 启动 MySQL、Redis（Redis 缺失时验证码会回退到内存，仅适合本地开发）
2. 执行 `database/mysql/simple_chat.sql` 创建数据库
3. 启动 `imchat-server`
4. 启动 `imchat-electron-client`

说明：

- 表结构由 Flyway 自动迁移，不再手工执行建表 SQL
- 迁移目录为 `imchat-server/src/main/resources/db/migration`
- 若当前库中已存在旧表结构，Flyway 会自动 baseline 接管
- 演示账号由 `V2__seed_demo_users.sql` 自动插入：`demo_alice` / `demo_bob`
- 演示账号默认密码：`Demo@123456`

发布前建议执行：

```powershell
$env:SPRING_DATASOURCE_USERNAME="your_db_user"
$env:SPRING_DATASOURCE_PASSWORD="your_db_password"
mvn -pl imchat-server -am -P release-check verify
```

空库或新环境建议先迁移再校验：

```powershell
mvn -f imchat-server/pom.xml -P release-check -Dimchat.flyway.name=your_db_name flyway:migrate
mvn -pl imchat-server -am -P release-check -Dimchat.flyway.name=your_db_name verify
```

启动服务端前，推荐先设置本机环境变量：

```powershell
$env:SPRING_DATASOURCE_USERNAME="your_db_user"
$env:SPRING_DATASOURCE_PASSWORD="your_db_password"
$env:JWT_SECRET="replace-with-your-own-jwt-secret-at-least-32-chars"
```

如果你要测试配置解密能力，再额外设置：

```powershell
$env:IMCHAT_CONFIG_KEY="your-own-local-config-key"
```

## 2. 认证流程

1. 调用 `GET /api/auth/captcha` 获取验证码
2. 使用验证码登录 `POST /api/auth/login`
3. 客户端携带 `Authorization: Bearer <token>` 调用受保护接口
4. WebSocket 连接后发送 `AUTH_REQUEST` 验证 token

预期：
- 登录成功后可拉取好友列表
- WebSocket 返回 `AUTH_RESPONSE` 成功

## 3. 好友系统联调

用两个账号 A/B 验证：

1. A 搜索 B（`GET /api/user/search`）
2. A 发起好友申请（`POST /api/friend/apply`）
3. B 在申请列表看到请求（`GET /api/friend/apply/list?status=0`）
4. B 同意申请（`POST /api/friend/apply/handle`）
5. A/B 均可在好友列表看到对方（`GET /api/friend/list`）
6. 验证删除、拉黑、免打扰接口

预期：
- 申请处理后双方好友关系同时生效
- 删除好友后双方关系同时移除

## 4. 消息模块联调

1. A/B 建立好友关系
2. A 发送单聊消息（WebSocket `PRIVATE_MESSAGE`）
3. B 在线时实时收到消息并回 ACK
4. B 离线时 A 发送消息，B 重新连接后收到离线补推
5. 查询历史消息（`GET /api/message/history/{friendId}`）
6. 查询会话摘要（`GET /api/message/conversations`）
7. 标记会话已读（`POST /api/message/read/{friendId}`）

预期：
- 实时消息与离线补推都可达
- 历史分页和会话摘要返回正确
- 已读后未读数减少

## 5. 已做优化点

- 加入统一异常处理，接口错误统一返回 `Result`
- 增加 MyBatis-Plus 分页插件，避免全量历史消息加载
- WebSocket 登录后批量补推离线消息
- 新增 CORS 配置，支持 Electron `file://` 场景请求 API
