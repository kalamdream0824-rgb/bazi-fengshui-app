# Server 后端

本目录是独立的 Spring Boot + Java 17 后端应用。前端代码、Node.js 和 Vite 不参与后端构建。

## 启动

```bash
./mvnw spring-boot:run
```

默认开发服务监听 `http://localhost:8080`，使用内存 H2 数据库。前端跨域地址通过 `APP_CORS_ORIGINS` 配置：

```bash
APP_CORS_ORIGINS=http://localhost:5173 ./mvnw spring-boot:run
```

使用 MySQL：

```bash
docker compose up -d
SPRING_PROFILES_ACTIVE=mysql ./mvnw spring-boot:run
```

## 常用命令

```bash
./mvnw test
./mvnw package
```

接口边界和请求/响应结构维护在仓库根目录的 `contracts/openapi.yaml`。
