const { app, BrowserWindow, ipcMain } = require("electron");
const path = require("path");

const APP_ENV = {
  apiBaseUrl: "http://127.0.0.1:8080/api",
  wsUrl: "ws://127.0.0.1:9000/ws"
};

function createWindow() {
  const mainWindow = new BrowserWindow({
    width: 1440,
    height: 900,
    minWidth: 1080,
    minHeight: 720,
    title: "IMChat Electron Client",
    webPreferences: {
      preload: path.join(__dirname, "preload.js"),
      contextIsolation: true,
      nodeIntegration: false
    }
  });

  mainWindow.loadFile(path.join(__dirname, "src/index.html"));
}

app.whenReady().then(() => {
  ipcMain.handle("app:get-env", () => APP_ENV);
  createWindow();

  app.on("activate", () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow();
    }
  });
});

app.on("window-all-closed", () => {
  if (process.platform !== "darwin") {
    app.quit();
  }
});
