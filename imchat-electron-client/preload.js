const { contextBridge, ipcRenderer } = require("electron");

contextBridge.exposeInMainWorld("imchat", {
  getEnv: () => ipcRenderer.invoke("app:get-env")
});
