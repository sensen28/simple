const MessageType = {
  AUTH_REQUEST: 1,
  AUTH_RESPONSE: 2,
  HEARTBEAT_REQUEST: 3,
  HEARTBEAT_RESPONSE: 4,
  PRIVATE_MESSAGE: 5,
  GROUP_MESSAGE: 6,
  MESSAGE_ACK: 7,
  FRIEND_APPLY_NOTICE: 8,
  FRIEND_STATUS_NOTICE: 9
};

const MAGIC = 0xcafebabe;

const state = {
  env: null,
  authMode: "login",
  captchaKey: "",
  accessToken: "",
  refreshToken: "",
  self: null,
  ws: null,
  heartbeatTimer: null,
  currentFriendId: null,
  friends: [],
  conversations: [],
  messagesByFriend: new Map()
};

const dom = {
  loginCard: document.getElementById("loginCard"),
  chatApp: document.getElementById("chatApp"),
  loginForm: document.getElementById("loginForm"),
  loginModeBtn: document.getElementById("loginModeBtn"),
  registerModeBtn: document.getElementById("registerModeBtn"),
  usernameInput: document.getElementById("usernameInput"),
  passwordInput: document.getElementById("passwordInput"),
  confirmPasswordField: document.getElementById("confirmPasswordField"),
  confirmPasswordInput: document.getElementById("confirmPasswordInput"),
  captchaInput: document.getElementById("captchaInput"),
  refreshCaptchaBtn: document.getElementById("refreshCaptchaBtn"),
  captchaImage: document.getElementById("captchaImage"),
  authSubmitBtn: document.getElementById("authSubmitBtn"),
  loginError: document.getElementById("loginError"),
  switchModeText: document.getElementById("switchModeText"),
  switchModeBtn: document.getElementById("switchModeBtn"),
  selfAvatar: document.getElementById("selfAvatar"),
  selfNickname: document.getElementById("selfNickname"),
  selfUsername: document.getElementById("selfUsername"),
  searchInput: document.getElementById("searchInput"),
  searchBtn: document.getElementById("searchBtn"),
  searchResult: document.getElementById("searchResult"),
  friendList: document.getElementById("friendList"),
  conversationList: document.getElementById("conversationList"),
  chatHeader: document.getElementById("chatHeader"),
  messageList: document.getElementById("messageList"),
  messageInput: document.getElementById("messageInput"),
  sendBtn: document.getElementById("sendBtn"),
  applyList: document.getElementById("applyList"),
  markReadBtn: document.getElementById("markReadBtn"),
  refreshAllBtn: document.getElementById("refreshAllBtn"),
  logoutBtn: document.getElementById("logoutBtn"),
  appHint: document.getElementById("appHint")
};

document.addEventListener("DOMContentLoaded", async () => {
  state.env = await window.imchat.getEnv();
  bindEvents();
  await refreshCaptcha();
});

function bindEvents() {
  dom.loginModeBtn.addEventListener("click", () => setAuthMode("login"));
  dom.registerModeBtn.addEventListener("click", () => setAuthMode("register"));
  dom.refreshCaptchaBtn.addEventListener("click", refreshCaptcha);
  dom.loginForm.addEventListener("submit", onAuthSubmit);
  dom.switchModeBtn.addEventListener("click", toggleAuthMode);
  dom.searchBtn.addEventListener("click", onSearchUser);
  dom.sendBtn.addEventListener("click", sendCurrentMessage);
  dom.markReadBtn.addEventListener("click", markCurrentConversationRead);
  dom.refreshAllBtn.addEventListener("click", refreshAllData);
  dom.logoutBtn.addEventListener("click", logout);
  setAuthMode("login");
}

async function requestApi(path, options = {}) {
  const method = options.method || "GET";
  const auth = options.auth !== false;
  const headers = {
    "Content-Type": "application/json"
  };
  if (auth && state.accessToken) {
    headers.Authorization = `Bearer ${state.accessToken}`;
  }
  const response = await fetch(`${state.env.apiBaseUrl}${path}`, {
    method,
    headers,
    body: options.body ? JSON.stringify(options.body) : undefined
  });

  let json;
  try {
    json = await response.json();
  } catch (error) {
    throw new Error(`服务返回异常（HTTP ${response.status}）`);
  }
  if (!response.ok || json.code !== 200) {
    throw new Error(json.message || `请求失败（HTTP ${response.status}）`);
  }
  return json.data;
}

async function refreshCaptcha() {
  try {
    const data = await requestApi("/auth/captcha", { auth: false });
    state.captchaKey = data.captchaKey;
    dom.captchaImage.src = data.captchaImage;
  } catch (error) {
    showAuthMessage(error.message, true);
  }
}

async function onAuthSubmit(event) {
  event.preventDefault();
  clearAuthMessage();
  if (state.authMode === "register") {
    await onRegister();
    return;
  }
  await onLogin();
}

async function onLogin() {

  try {
    const payload = {
      username: dom.usernameInput.value.trim(),
      password: dom.passwordInput.value,
      captcha: dom.captchaInput.value.trim(),
      captchaKey: state.captchaKey
    };
    const data = await requestApi("/auth/login", {
      method: "POST",
      body: payload,
      auth: false
    });
    state.accessToken = data.accessToken;
    state.refreshToken = data.refreshToken;
    state.self = {
      userId: data.userId,
      username: data.username,
      nickname: data.nickname || data.username
    };

    dom.selfAvatar.textContent = (state.self.nickname || "ME").slice(0, 2).toUpperCase();
    dom.selfNickname.textContent = state.self.nickname;
    dom.selfUsername.textContent = `@${state.self.username}`;

    dom.loginCard.classList.add("hidden");
    dom.chatApp.classList.remove("hidden");
    showHint("登录成功，正在连接 WebSocket...");

    connectWebSocket();
    await refreshAllData();
  } catch (error) {
    showAuthMessage(error.message, true);
    await refreshCaptcha();
  }
}

async function onRegister() {
  const username = dom.usernameInput.value.trim();
  const password = dom.passwordInput.value;
  const confirmPassword = dom.confirmPasswordInput.value;
  const captcha = dom.captchaInput.value.trim();

  if (!username || !password || !confirmPassword || !captcha) {
    showAuthMessage("请完整填写注册信息", true);
    return;
  }
  if (password !== confirmPassword) {
    showAuthMessage("两次密码输入不一致", true);
    return;
  }

  try {
    await requestApi("/auth/register", {
      method: "POST",
      body: {
        username,
        password,
        confirmPassword,
        captcha,
        captchaKey: state.captchaKey
      },
      auth: false
    });
    setAuthMode("login");
    dom.captchaInput.value = "";
    dom.confirmPasswordInput.value = "";
    showAuthMessage("注册成功，请使用新账号登录", false);
    await refreshCaptcha();
  } catch (error) {
    showAuthMessage(error.message, true);
    await refreshCaptcha();
  }
}

function connectWebSocket() {
  closeWebSocket();
  state.ws = new WebSocket(state.env.wsUrl);
  state.ws.binaryType = "arraybuffer";

  state.ws.onopen = () => {
    sendWsMessage(MessageType.AUTH_REQUEST, {
      token: state.accessToken
    });
    startHeartbeat();
  };

  state.ws.onmessage = async (event) => {
    let buffer = event.data;
    if (buffer instanceof Blob) {
      buffer = await buffer.arrayBuffer();
    }
    if (!(buffer instanceof ArrayBuffer)) {
      return;
    }
    const protocol = decodeProtocol(buffer);
    if (!protocol) {
      return;
    }
    handleWsMessage(protocol);
  };

  state.ws.onclose = () => {
    stopHeartbeat();
    showHint("WebSocket 已断开");
  };

  state.ws.onerror = () => {
    showHint("WebSocket 连接异常，请检查服务端");
  };
}

function closeWebSocket() {
  stopHeartbeat();
  if (state.ws) {
    try {
      state.ws.close();
    } catch (error) {
      console.error(error);
    }
  }
  state.ws = null;
}

function startHeartbeat() {
  stopHeartbeat();
  state.heartbeatTimer = window.setInterval(() => {
    sendWsMessage(MessageType.HEARTBEAT_REQUEST, {});
  }, 20000);
}

function stopHeartbeat() {
  if (state.heartbeatTimer) {
    window.clearInterval(state.heartbeatTimer);
    state.heartbeatTimer = null;
  }
}

function handleWsMessage(protocol) {
  const payload = protocol.payload || {};
  switch (protocol.messageType) {
    case MessageType.AUTH_RESPONSE:
      if (protocol.status === 0) {
        showHint("WebSocket 认证成功");
      } else {
        showHint(payload.msg || "WebSocket 认证失败");
      }
      break;
    case MessageType.PRIVATE_MESSAGE:
      onPrivateMessage(payload);
      break;
    case MessageType.MESSAGE_ACK:
      if (payload.code && payload.code !== 200) {
        showHint(payload.msg || "消息发送失败");
      }
      break;
    case MessageType.FRIEND_APPLY_NOTICE:
    case MessageType.FRIEND_STATUS_NOTICE:
      refreshAllData();
      break;
    default:
      break;
  }
}

function onPrivateMessage(payload) {
  const fromUserId = Number(payload.fromUserId);
  const toUserId = Number(payload.toUserId);
  const friendId = fromUserId === state.self.userId ? toUserId : fromUserId;
  const message = {
    msgId: payload.msgId || `tmp-${Date.now()}`,
    fromUserId,
    toUserId,
    content: payload.content || "",
    type: payload.type || 1,
    status: 0,
    createTime: toIsoString(payload.timestamp)
  };
  const list = state.messagesByFriend.get(friendId) || [];
  list.push(message);
  state.messagesByFriend.set(friendId, list);

  if (fromUserId !== state.self.userId && payload.msgId) {
    sendWsMessage(MessageType.MESSAGE_ACK, { msgId: payload.msgId });
  }
  if (state.currentFriendId === friendId) {
    renderMessages(friendId);
  }
  refreshConversations();
}

async function refreshAllData() {
  await Promise.all([refreshFriends(), refreshApplyList(), refreshConversations()]);
}

async function refreshFriends() {
  try {
    state.friends = await requestApi("/friend/list");
    renderFriendList();
  } catch (error) {
    showHint(error.message);
  }
}

function renderFriendList() {
  dom.friendList.innerHTML = "";
  if (!state.friends.length) {
    dom.friendList.innerHTML = `<div class="item-sub">暂无好友，先去搜索添加</div>`;
    return;
  }
  state.friends.forEach((friend) => {
    const item = document.createElement("button");
    item.className = `list-item ${state.currentFriendId === friend.friendId ? "active" : ""}`;
    item.innerHTML = `
      <div>
        <div class="item-name">${escapeHtml(friend.nickname || friend.username)}</div>
        <div class="item-sub">@${escapeHtml(friend.username || "")}</div>
      </div>
      <div class="item-sub">${friend.status === 1 ? "在线" : "离线"}</div>
    `;
    item.addEventListener("click", () => openFriendChat(friend.friendId));
    dom.friendList.appendChild(item);
  });
}

async function openFriendChat(friendId) {
  state.currentFriendId = friendId;
  renderFriendList();
  const friend = state.friends.find((item) => item.friendId === friendId);
  dom.chatHeader.textContent = friend
    ? `与 ${friend.nickname || friend.username} 的对话`
    : `与用户 ${friendId} 的对话`;

  try {
    const page = await requestApi(`/message/history/${friendId}?pageNo=1&pageSize=100`);
    state.messagesByFriend.set(friendId, page.records || []);
    renderMessages(friendId);
    await markCurrentConversationRead();
  } catch (error) {
    showHint(error.message);
  }
}

function renderMessages(friendId) {
  dom.messageList.innerHTML = "";
  const records = state.messagesByFriend.get(friendId) || [];
  if (!records.length) {
    dom.messageList.innerHTML = `<div class="item-sub">暂无聊天记录</div>`;
    return;
  }
  records.forEach((msg) => {
    const inbound = Number(msg.fromUserId) !== state.self.userId;
    const node = document.createElement("div");
    node.className = `message ${inbound ? "inbound" : "outbound"}`;
    node.innerHTML = `
      <div>${escapeHtml(msg.content || "")}</div>
      <div class="message-time">${formatTime(msg.createTime)}</div>
    `;
    dom.messageList.appendChild(node);
  });
  dom.messageList.scrollTop = dom.messageList.scrollHeight;
}

async function sendCurrentMessage() {
  if (!state.currentFriendId) {
    showHint("请先选择一个好友");
    return;
  }
  const content = dom.messageInput.value.trim();
  if (!content) {
    return;
  }
  if (!state.ws || state.ws.readyState !== WebSocket.OPEN) {
    showHint("WebSocket 尚未连接");
    return;
  }

  sendWsMessage(MessageType.PRIVATE_MESSAGE, {
    toUserId: state.currentFriendId,
    content,
    type: 1
  });

  const list = state.messagesByFriend.get(state.currentFriendId) || [];
  list.push({
    msgId: `local-${Date.now()}`,
    fromUserId: state.self.userId,
    toUserId: state.currentFriendId,
    content,
    type: 1,
    status: 0,
    createTime: new Date().toISOString()
  });
  state.messagesByFriend.set(state.currentFriendId, list);
  dom.messageInput.value = "";
  renderMessages(state.currentFriendId);
}

async function onSearchUser() {
  const keyword = dom.searchInput.value.trim();
  if (!keyword) {
    return;
  }
  try {
    const users = await requestApi(`/user/search?keyword=${encodeURIComponent(keyword)}`);
    renderSearchResult(users);
  } catch (error) {
    showHint(error.message);
  }
}

function renderSearchResult(users) {
  dom.searchResult.innerHTML = "";
  if (!users.length) {
    dom.searchResult.innerHTML = `<div class="item-sub">无匹配用户</div>`;
    return;
  }
  users.forEach((user) => {
    const item = document.createElement("div");
    item.className = "list-item";
    item.innerHTML = `
      <div>
        <div class="item-name">${escapeHtml(user.nickname || user.username)}</div>
        <div class="item-sub">@${escapeHtml(user.username || "")}</div>
      </div>
    `;
    const addBtn = document.createElement("button");
    addBtn.className = "btn small";
    addBtn.textContent = "加好友";
    addBtn.addEventListener("click", () => applyFriend(user.userId));
    item.appendChild(addBtn);
    dom.searchResult.appendChild(item);
  });
}

async function applyFriend(targetUserId) {
  try {
    await requestApi("/friend/apply", {
      method: "POST",
      body: {
        targetUserId,
        remark: "来自 Electron 客户端"
      }
    });
    showHint("好友申请已发送");
  } catch (error) {
    showHint(error.message);
  }
}

async function refreshApplyList() {
  try {
    const applies = await requestApi("/friend/apply/list?status=0");
    dom.applyList.innerHTML = "";
    if (!applies.length) {
      dom.applyList.innerHTML = `<div class="item-sub">暂无待处理申请</div>`;
      return;
    }
    applies.forEach((apply) => {
      const item = document.createElement("div");
      item.className = "list-item";
      item.innerHTML = `
        <div>
          <div class="item-name">${escapeHtml(apply.applyNickname || apply.applyUsername || "未知用户")}</div>
          <div class="item-sub">${escapeHtml(apply.remark || "")}</div>
        </div>
      `;

      const actions = document.createElement("div");
      actions.className = "apply-actions";

      const agreeBtn = document.createElement("button");
      agreeBtn.className = "btn small";
      agreeBtn.textContent = "同意";
      agreeBtn.addEventListener("click", () => handleApply(apply.applyId, 1));

      const rejectBtn = document.createElement("button");
      rejectBtn.className = "btn small secondary";
      rejectBtn.textContent = "拒绝";
      rejectBtn.addEventListener("click", () => handleApply(apply.applyId, 2));

      actions.appendChild(agreeBtn);
      actions.appendChild(rejectBtn);
      item.appendChild(actions);
      dom.applyList.appendChild(item);
    });
  } catch (error) {
    showHint(error.message);
  }
}

async function handleApply(applyId, operateType) {
  try {
    await requestApi("/friend/apply/handle", {
      method: "POST",
      body: {
        applyId,
        operateType
      }
    });
    await refreshAllData();
  } catch (error) {
    showHint(error.message);
  }
}

async function refreshConversations() {
  try {
    state.conversations = await requestApi("/message/conversations?limit=20");
    dom.conversationList.innerHTML = "";
    if (!state.conversations.length) {
      dom.conversationList.innerHTML = `<div class="item-sub">暂无会话</div>`;
      return;
    }
    state.conversations.forEach((conversation) => {
      const item = document.createElement("button");
      item.className = "list-item";
      item.innerHTML = `
        <div>
          <div class="item-name">${escapeHtml(conversation.friendNickname || conversation.friendUsername || "未知用户")}</div>
          <div class="item-sub">${escapeHtml(conversation.lastContent || "")}</div>
        </div>
        <div class="item-sub">${conversation.unreadCount > 0 ? `${conversation.unreadCount} 未读` : "已读"}</div>
      `;
      item.addEventListener("click", () => openFriendChat(conversation.friendId));
      dom.conversationList.appendChild(item);
    });
  } catch (error) {
    showHint(error.message);
  }
}

async function markCurrentConversationRead() {
  if (!state.currentFriendId) {
    return;
  }
  try {
    await requestApi(`/message/read/${state.currentFriendId}`, {
      method: "POST"
    });
    refreshConversations();
  } catch (error) {
    showHint(error.message);
  }
}

function sendWsMessage(messageType, payload) {
  if (!state.ws || state.ws.readyState !== WebSocket.OPEN) {
    return;
  }
  const buffer = encodeProtocol(messageType, 0, payload || {});
  state.ws.send(buffer);
}

function encodeProtocol(messageType, status, payload) {
  const encoder = new TextEncoder();
  const bodyBytes = encoder.encode(JSON.stringify(payload || {}));
  const buffer = new ArrayBuffer(12 + bodyBytes.length);
  const view = new DataView(buffer);
  view.setUint32(0, MAGIC);
  view.setUint8(4, 1);
  view.setUint8(5, messageType);
  view.setUint8(6, 0);
  view.setUint8(7, status || 0);
  view.setUint32(8, bodyBytes.length);
  new Uint8Array(buffer, 12).set(bodyBytes);
  return buffer;
}

function decodeProtocol(buffer) {
  if (!buffer || buffer.byteLength < 12) {
    return null;
  }
  const view = new DataView(buffer);
  const magic = view.getUint32(0);
  if (magic !== MAGIC) {
    return null;
  }
  const messageType = view.getUint8(5);
  const status = view.getUint8(7);
  const length = view.getUint32(8);
  if (buffer.byteLength < 12 + length) {
    return null;
  }
  const bodyBytes = new Uint8Array(buffer, 12, length);
  const bodyStr = new TextDecoder().decode(bodyBytes);
  let payload = {};
  if (bodyStr) {
    try {
      payload = JSON.parse(bodyStr);
    } catch (error) {
      payload = {};
    }
  }
  return {
    messageType,
    status,
    payload
  };
}

function formatTime(value) {
  if (!value) {
    return "";
  }
  let date = null;
  if (typeof value === "number") {
    date = new Date(value);
  } else if (typeof value === "string") {
    const normalized = value.includes("T") ? value : value.replace(" ", "T");
    date = new Date(normalized);
  } else {
    date = new Date(value);
  }
  if (Number.isNaN(date.getTime())) {
    return "";
  }
  return `${date.getHours().toString().padStart(2, "0")}:${date
    .getMinutes()
    .toString()
    .padStart(2, "0")}`;
}

function toIsoString(timestamp) {
  if (!timestamp) {
    return new Date().toISOString();
  }
  const numeric = Number(timestamp);
  const date = Number.isNaN(numeric) ? new Date(timestamp) : new Date(numeric);
  if (Number.isNaN(date.getTime())) {
    return new Date().toISOString();
  }
  return date.toISOString();
}

function showHint(text) {
  dom.appHint.textContent = text;
}

function clearAuthMessage() {
  dom.loginError.textContent = "";
  dom.loginError.classList.remove("success");
}

function showAuthMessage(text, isError) {
  dom.loginError.textContent = text || "";
  dom.loginError.classList.toggle("success", !isError && Boolean(text));
}

function toggleAuthMode() {
  setAuthMode(state.authMode === "login" ? "register" : "login");
}

function setAuthMode(mode) {
  state.authMode = mode;
  const isRegister = mode === "register";
  dom.loginModeBtn.classList.toggle("active", !isRegister);
  dom.registerModeBtn.classList.toggle("active", isRegister);
  dom.confirmPasswordField.classList.toggle("hidden-field", !isRegister);
  dom.confirmPasswordInput.required = isRegister;
  dom.authSubmitBtn.textContent = isRegister ? "注册" : "登录";
  dom.switchModeText.textContent = isRegister ? "已有账号？" : "还没有账号？";
  dom.switchModeBtn.textContent = isRegister ? "返回登录" : "立即注册";
  clearAuthMessage();
}

function escapeHtml(value) {
  return String(value || "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

function logout() {
  closeWebSocket();
  state.accessToken = "";
  state.refreshToken = "";
  state.self = null;
  state.friends = [];
  state.conversations = [];
  state.currentFriendId = null;
  state.messagesByFriend.clear();

  dom.chatApp.classList.add("hidden");
  dom.loginCard.classList.remove("hidden");
  dom.loginForm.reset();
  setAuthMode("login");
  dom.friendList.innerHTML = "";
  dom.conversationList.innerHTML = "";
  dom.messageList.innerHTML = "";
  dom.applyList.innerHTML = "";
  dom.searchResult.innerHTML = "";
  dom.chatHeader.textContent = "请选择一个好友开始聊天";
  showHint("");
  refreshCaptcha();
}
