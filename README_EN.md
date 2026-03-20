# IMChat 2.0

<div align="right">
  <a href="./README.md">简体中文</a> | <strong>English</strong>
</div>

## Overview

IMChat 2.0 is an instant messaging demo project built on OpenJDK 8. It includes:

- `imchat-server`: backend service (Spring Boot + Netty)
- `imchat-common`: shared protocol/DTO/VO/util classes
- `imchat-electron-client`: desktop client built with Electron (replaces the old Swing client)

> Note: the `backup` directory contains legacy snapshots and is not part of the active build.

## Tech Stack

- OpenJDK 8
- Spring Boot 2.7.x
- Spring Security + JWT
- MyBatis-Plus
- Netty WebSocket
- MySQL 8.x
- Redis 6.x
- Electron

## Implemented Features

### 1. Authentication

- Captcha
- Register/Login
- JWT-based auth

### 2. Friend System

- Search users by username/nickname
- Send friend requests
- View pending requests
- Accept/Reject requests
- Friend list
- Delete friend
- Block/Unblock
- Mute settings

### 3. Messaging

- Realtime 1-to-1 messaging via WebSocket
- Offline message push after reconnect
- Paginated message history
- Conversation summary list
- Read status (conversation read + per-message ACK)

### 4. Electron Client

- Captcha login
- Friend search/request handling
- Friend list + conversation list
- Realtime messaging
- Message history viewer

## Project Structure

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

## Requirements

- OpenJDK 8 (required)
- Maven 3.6+
- Node.js 18+
- MySQL 8.x
- Redis 6.x (optional, captcha falls back to in-memory storage for local development)

## Quick Start

### 1. Initialize Database

Create the database first:

```bash
mysql -u your_db_user -p < database/mysql/simple_chat.sql
```

Then let Flyway execute schema migrations automatically when the backend starts:

```text
imchat-server/src/main/resources/db/migration
```

Current initial migration:

```text
V1__init_simple_chat_schema.sql
```

Demo user seed migration:

```text
V2__seed_demo_users.sql
```

### 2. Configure Backend

Update:

```text
imchat-server/src/main/resources/application.yml
```

The default database name is now `simple_chat`.

The schema is now managed by Flyway:

- `database/mysql/simple_chat.sql` only creates the database
- Table definitions and future schema changes live under `db/migration`
- For later changes, add `V2__...sql`, `V3__...sql`, and avoid manual schema edits

Flyway also seeds two demo accounts automatically:

- Username: `demo_alice`
- Username: `demo_bob`
- Default password: `Demo@123456`

Notes:

- Demo accounts are intended for local development and integration testing only
- If you do not need them, remove or override them in a later migration

The repository now keeps only sample encrypted placeholders in `application.yml`. Real DB credentials and JWT secrets should not be committed.

Recommended approach: inject all real secrets through local environment variables:

```powershell
$env:SPRING_DATASOURCE_USERNAME="your_db_user"
$env:SPRING_DATASOURCE_PASSWORD="your_db_password"
$env:JWT_SECRET="replace-with-your-own-jwt-secret-at-least-32-chars"
```

If you need to override host, port, or database name:

```powershell
$env:IMCHAT_DB_HOST="localhost"
$env:IMCHAT_DB_PORT="3306"
$env:IMCHAT_DB_NAME="simple_chat"
```

If you prefer encrypted values inside the config file, generate them locally:

```powershell
$env:IMCHAT_CONFIG_KEY="your-own-local-config-key"
.\scripts\encrypt-config.ps1 -Key $env:IMCHAT_CONFIG_KEY -Value "your-secret"
```

Then replace the target fields in `application.yml` with the generated `ENC(...)` values.

Notes:

- Keep `IMCHAT_CONFIG_KEY` in your local environment only
- Before pushing to GitHub, verify that no real credentials were written into tracked files

### 3. Start Backend

```bash
mvn -pl imchat-server -am spring-boot:run
```

On first startup, Flyway will create the tables automatically. If the database already contains the older manually created schema, Flyway will baseline it and start tracking it via `flyway_schema_history`.

Default endpoints:

- HTTP API: `http://127.0.0.1:8080/api`
- WebSocket: `ws://127.0.0.1:9000/ws`

### 4. Maven / Release Validation

Validate an already managed database:

```powershell
$env:SPRING_DATASOURCE_USERNAME="your_db_user"
$env:SPRING_DATASOURCE_PASSWORD="your_db_password"
mvn -pl imchat-server -am -P release-check verify
```

For an empty or brand-new database, migrate first and then validate:

```powershell
mvn -f imchat-server/pom.xml -P release-check -Dimchat.flyway.name=your_db_name flyway:migrate
mvn -pl imchat-server -am -P release-check -Dimchat.flyway.name=your_db_name verify
```

The repository also includes a GitHub Actions workflow:

- `.github/workflows/release-check.yml`

It automatically runs:

- `flyway:migrate` against an empty CI database
- Maven build plus `flyway:validate`

### 5. Start Electron Client

```bash
cd imchat-electron-client
npm install
npm run start
```

## Main APIs (Selected)

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

## Integration Testing

See:

- `docs/integration-test.md`
