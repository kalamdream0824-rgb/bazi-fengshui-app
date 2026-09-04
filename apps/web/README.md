# Web 前端

本目录是独立的 React + Vite 前端应用，不依赖 Java/Maven 才能进行 UI 开发。

## 启动

```bash
npm install
npm run dev
```

默认使用本地 Mock 数据。需要联调后端时：

```bash
VITE_API_MODE=http npm run dev
```

默认 `/api` 请求会由 Vite 代理到 `http://localhost:8080`。如果前后端部署在不同域名，设置 `VITE_API_BASE_URL` 为后端 API 根地址，并在后端配置允许的前端 Origin。

## 常用命令

```bash
npm run build
npm test
npm run lint
```

前端只通过 `contracts/openapi.yaml` 约定的 REST API 与后端通信。
