# IMChat Electron Client

## 运行方式

1. 安装依赖

```bash
npm install
```

2. 启动客户端

```bash
npm run start
```

## 默认连接地址

- HTTP API: `http://127.0.0.1:8080/api`
- WebSocket: `ws://127.0.0.1:9000/ws`

可在 `main.js` 的 `APP_ENV` 中调整。

## 已实现能力

- 验证码登录
- 好友搜索与申请
- 待处理好友申请同意/拒绝
- 好友列表与最近会话
- 单聊实时消息（WebSocket）
- 历史消息查询与会话已读标记
