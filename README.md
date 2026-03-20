# IMChat - 基于Netty的即时聊天项目

基于netty的即时聊天项目
网络协议：TCP/IP
代码量：约2100行
消息封装：JSON

## 项目结构
```
imchat
├── imchat-common    # 公共模块，存放客户端和服务端共享的代码
├── imchat-client   # 聊天客户端（Swing + Netty）
└── imchat-server   # 聊天服务端（Netty + MySQL）
```

## 技术栈
- **网络通信**: Netty 4.1.x
- **序列化**: Jackson 2.15.x
- **数据库**: MySQL 8.x
- **连接池**: C3P0 0.9.5.x
- **日志**: Log4j 1.2.x
- **邮件**: Apache Commons Email 1.5
- **JDK版本**: 1.8+

## 功能点介绍

主要开发的功能模块：用户账号模块、聊天模块、文件传输模块

### 1、用户模块
- 1.1、用户登录模块
- 1.2、用户的注册模块
- 1.3、忘记密码
- 1.4、修改密码
### 2、聊天模块
- 2.1、单聊（一对一聊天）
- 2.2、群聊
- 2.3、好友列表
- 2.4、好友上下线通知
### 3、文件传输
- 3.1、指定用户发送文件
- 3.2、群发文件

## 快速开始

### 1. 环境准备
- JDK 1.8+
- Maven 3.6+
- MySQL 8.0+

### 2. 数据库初始化
创建数据库并执行SQL脚本：
```sql
CREATE DATABASE imchat;
USE imchat;

-- 用户表
CREATE TABLE user (
    username VARCHAR(255) PRIMARY KEY,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    ip VARCHAR(255),
    status INT DEFAULT 0
);

-- 消息表
CREATE TABLE message (
    sender VARCHAR(255) NOT NULL,
    receiver VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    status INT DEFAULT 0,
    allName VARCHAR(255)
);
```

### 3. 配置数据库
修改 `imchat-server/src/main/resources/c3p0-config.xml` 中的数据库连接信息。

### 4. 编译打包
```bash
mvn clean package
```

### 5. 运行
- 先启动服务端：
```bash
java -jar imchat-server/target/imchat-server-1.0.0-SNAPSHOT.jar
```
- 再启动客户端：
```bash
java -jar imchat-client/target/imchat-client-1.0.0-SNAPSHOT.jar
```
