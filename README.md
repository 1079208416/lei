# 食刻外卖 MVP

单城市、多商户外卖平台 MVP，包含用户点餐、商家履约、骑手配送和平台运营四个角色。

## 已实现

- 用户端：附近商家、菜单规格、购物车、地址、下单、模拟支付、订单跟踪、取消和退款申请。
- 商家端：经营概览、订单提醒、接单、拒单、出餐、菜单分类、商品和售罄管理。
- 骑手端：任务列表、取餐、送达、联系用户和地图跳转。
- 运营端：平台数据、订单查询、人工派单、退款审核和商户营业状态管理。
- 后端：JWT 登录、角色权限、订单状态机、支付模拟、退款、WebSocket 实时通知。
- 本地开发默认使用文件型 H2，生产配置支持 MySQL。

## 技术栈

- 后端：Java 17、Spring Boot 3、Spring Security、MyBatis-Plus、Flyway、H2、MySQL。
- 前端：Vue 3、TypeScript、Vite、Pinia、Vue Router、Lucide Icons。
- 部署：Nginx、Docker Compose。

## 快速启动

环境要求：

- JDK 17 或 21。
- Node.js 20 以上。

在 PowerShell 中运行：

```powershell
.\scripts\start-dev.ps1
```

启动完成后访问：

- 前端：http://localhost:5173
- 后端：http://localhost:18080
- H2 控制台：http://localhost:18080/h2-console

停止本地服务：

```powershell
.\scripts\stop-dev.ps1
```

## 在 IDEA 中运行

项目已经附带三个共享运行配置，打开项目并等待 Maven 与 Node.js 索引完成后，在右上角运行配置中选择：

- `Shike Full Stack`：同时启动前后端。
- `Shike Backend`：只启动 Spring Boot 后端，端口为 `18080`。
- `Shike Frontend`：只启动 Vite 前端。

如果 IDEA 没有立即显示这些配置，执行 `File -> Reload from Disk`，或重启项目窗口。

启动后访问 http://localhost:5173。

## 演示账号

登录页可直接选择角色进入，也可以使用以下账号，统一密码为 `demo123`：

| 角色 | 手机号 |
| --- | --- |
| 用户 | `13800000001` |
| 商家 | `13800000002` |
| 骑手 | `13800000003` |
| 平台运营 | `13800000004` |

## 手动启动

后端：

```powershell
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=18080"
```

前端：

```powershell
cd frontend
$env:VITE_API_PROXY_TARGET="http://localhost:18080"
npm install
npm run dev
```

如果使用默认后端端口 `8080`，前端不需要设置 `VITE_API_PROXY_TARGET`。

## 构建与测试

后端完整业务链路测试：

```powershell
cd backend
.\mvnw.cmd test
```

前端生产构建：

```powershell
cd frontend
npm run build
```

## MySQL 与 Docker

```powershell
docker compose -f deploy/docker-compose.yml up --build
```

启动后访问 http://localhost:5173。MySQL 数据保存在 Docker 卷中。

## 开发说明

- 开发环境支付为模拟支付，生产环境需要替换为微信支付 API v3 适配器。
- 当前地图功能使用外部导航搜索链接，正式环境可接入腾讯位置服务。
- 短信、微信订阅消息和对象存储尚未接入真实云服务。
- H2 数据文件位于 `backend/data`，删除后下次启动会重新生成演示数据。
- 微信支付、商家结算和真实地图能力上线前，需要补充对应商户资质和云服务密钥。

## 项目结构

```text
backend/    Spring Boot 后端
frontend/   Vue 3 四角色前端
docs/       产品设计与技术方案
deploy/     Docker 部署配置
scripts/    本地启动与停止脚本
```
