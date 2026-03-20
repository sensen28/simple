# IMChat 2.0

<div align="right">
  <strong>简体中文</strong> | <a href="./README_EN.md">English</a>
</div>

## 项目简介

IMChat 2.0 是一个基于 OpenJDK 8 的即时通讯示例项目，当前包含：

- `imchat-server`：Spring Boot + Netty 的服务端
- `imchat-common`：服务端与客户端共享的协议/DTO/VO/工具
- `imchat-electron-client`：Electron 桌面客户端（替代旧 Swing 客户端）

> 说明：`backup` 目录为历史备份代码，不参与当前版本构建与运行。

## 技术栈

- OpenJDK 8
- Spring Boot 2.7.x
- Spring Security + JWT
- MyBatis-Plus
- Netty WebSocket
- MySQL 8.x
- Redis 6.x
- Electron

## 已实现功能

### 1. 用户认证

- 图形验证码
- 注册/登录
- JWT 鉴权

### 2. 好友系统

- 按用户名/昵称搜索用户
- 发起好友申请
- 查看待处理申请
- 同意/拒绝申请
- 好友列表
- 删除好友
- 拉黑/取消拉黑
- 免打扰设置

### 3. 消息模块

- 单聊实时消息（WebSocket）
- 离线消息补推（用户连接后）
- 历史消息分页
- 最近会话摘要
- 消息已读（会话已读 + 单条 ACK）

### 4. Electron 客户端

- 验证码登录
- 好友搜索与申请处理
- 好友列表与会话列表
- 消息实时收发
- 历史消息查看

## 项目结构

```text
simple
├── database
│   └── mysql
│       └── simple_chat.sql
├── imchat-common
├── imchat-server
│   └── src/main/resources/db/migration
│       └── V1__init_simple_chat_schema.sql
├── imchat-electron-client
├── docs
│   └── integration-test.md
├── scripts
│   └── encrypt-config.ps1
└── backup
```

## 环境要求

- OpenJDK 8（必须）
- Maven 3.6+
- Node.js 18+
- MySQL 8.x
- Redis 6.x（可选，本地开发缺失时验证码会回退到内存）

## 快速开始

### 1. 初始化数据库

先创建数据库：

```bash
mysql -u your_db_user -p < database/mysql/simple_chat.sql
```

然后由 Flyway 在服务端启动时自动执行迁移脚本：

```text
imchat-server/src/main/resources/db/migration
```

当前首个迁移文件：

```text
V1__init_simple_chat_schema.sql
```

演示账号种子迁移：

```text
V2__seed_demo_users.sql
```

### 2. 配置服务端

修改配置文件：

```text
imchat-server/src/main/resources/application.yml
```

当前默认数据库名已切换为 `simple_chat`。

数据库结构现在由 Flyway 管理：

- `database/mysql/simple_chat.sql` 只负责创建数据库
- 表结构和后续字段变更统一放到 `db/migration` 目录
- 后续升级请新增 `V2__...sql`、`V3__...sql`，不要再手改线上库结构

Flyway 会自动插入两个演示账号：

- 用户名：`demo_alice`
- 用户名：`demo_bob`
- 默认密码：`Demo@123456`

说明：

- 演示账号仅用于本地开发与联调
- 如不需要，可在后续迁移中删除或覆盖

仓库中的 `application.yml` 只保留示例密文，不再保存真实数据库账号、数据库密码和 JWT 密钥。

推荐做法：真实敏感信息全部通过本机环境变量注入，不提交到 GitHub：

```powershell
$env:SPRING_DATASOURCE_USERNAME="your_db_user"
$env:SPRING_DATASOURCE_PASSWORD="your_db_password"
$env:JWT_SECRET="replace-with-your-own-jwt-secret-at-least-32-chars"
```

如需调整数据库主机、端口或库名，也可以额外设置：

```powershell
$env:IMCHAT_DB_HOST="localhost"
$env:IMCHAT_DB_PORT="3306"
$env:IMCHAT_DB_NAME="simple_chat"
```

如果你希望把自己的密文写回配置文件，可以本地生成：

```powershell
$env:IMCHAT_CONFIG_KEY="your-own-local-config-key"
.\scripts\encrypt-config.ps1 -Key $env:IMCHAT_CONFIG_KEY -Value "your-secret"
```

然后把生成的 `ENC(...)` 替换到 `application.yml` 对应字段中。

注意：

- `IMCHAT_CONFIG_KEY` 只保留在本机环境中，不要提交到仓库
- 推送到 GitHub 前，确认没有把本地账号密码写入任何配置文件

### 3. 启动服务端

```bash
mvn -pl imchat-server -am spring-boot:run
```

首次启动时，Flyway 会自动完成建表；如果库中已经存在旧表结构，则会自动接管并写入 `flyway_schema_history`，避免重复执行初始化脚本。

默认地址：

- HTTP API：`http://127.0.0.1:8080/api`
- WebSocket：`ws://127.0.0.1:9000/ws`

### 4. Maven / 发布校验

已接管数据库校验：

```powershell
$env:SPRING_DATASOURCE_USERNAME="your_db_user"
$env:SPRING_DATASOURCE_PASSWORD="your_db_password"
mvn -pl imchat-server -am -P release-check verify
```

空库或新环境建议先执行迁移，再做校验：

```powershell
mvn -f imchat-server/pom.xml -P release-check -Dimchat.flyway.name=your_db_name flyway:migrate
mvn -pl imchat-server -am -P release-check -Dimchat.flyway.name=your_db_name verify
```

仓库已补充 GitHub Actions：

- `.github/workflows/release-check.yml`

它会在 GitHub 上自动完成：

- 对空数据库执行 `flyway:migrate`
- Maven 编译与 `flyway:validate`

### 5. 启动 Electron 客户端

```bash
cd imchat-electron-client
npm install
npm run start
```

## 主要接口（节选）

- `GET /api/auth/captcha`
- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/user/search?keyword=xxx`
- `POST /api/friend/apply`
- `GET /api/friend/apply/list`
- `POST /api/friend/apply/handle`
- `GET /api/friend/list`
- `DELETE /api/friend/{friendId}`
- `POST /api/message/send`
- `GET /api/message/history/{friendId}`
- `GET /api/message/conversations`
- `POST /api/message/read/{friendId}`

## 联调测试

联调用例与步骤见：

- `docs/integration-test.md`
