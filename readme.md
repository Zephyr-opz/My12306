# My12306

My12306 是一个前后端分离的铁路售票课设项目：后端使用 Spring Boot、MyBatis-Plus 和 MySQL，前端使用 Vue 3、Vue Router、Pinia、Element Plus 和 Vite。

## 主要功能

- 用户注册、登录和管理员权限管理
- 车次模板维护、经停站排序、运行日期范围和停用/恢复
- 后端计算站点到发时间和按乘车时间比例生成区间票价
- 车次模板预览、返回修改和确认保存
- 地区/车站简单维护，以及模板站点筛选选择
- 车票查询、动态区间座位库存、购票、取消和退票
- 订单查询和车票详细信息展示

## 目录结构

```text
My12306/
├─ backend/       Spring Boot 后端和数据库脚本
├─ frontend/      Vue 3 前端
├─ readme.md
└─ .gitignore
```

## 环境要求

- JDK 17 或更高版本
- Maven 3.6 或更高版本
- Node.js 18 或更高版本
- MySQL 8.0 或更高版本

## 数据库初始化

先创建数据库：

```sql
CREATE DATABASE IF NOT EXISTS my12306
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

新建数据库按以下顺序执行脚本：

1. `backend/db/migration/V0__baseline_schema.sql`
2. `backend/db/migration/V1__4_3_3_core_schema.sql`
3. `backend/db/migration/V2__dynamic_seat_inventory.sql`
4. `backend/db/seed/V1__demo_train_data.sql`（需要演示车次和运行计划时执行）

`backend/my12306.sql` 是原始兼容脚本。初始化时不要和 `V0__baseline_schema.sql` 同时执行，避免重复建表。

## 后端运行

首次克隆项目后，在后端目录创建本地配置：

PowerShell：

```powershell
cd backend
Copy-Item src/main/resources/application-example.yml src/main/resources/application.yml
```

然后编辑 `backend/src/main/resources/application.yml`，填写本机 MySQL 用户名、密码和 JWT 密钥。这个文件已加入 `.gitignore`，不要提交到远端。

安装依赖并运行测试：

```powershell
cd backend
mvn clean test
```

启动后端：

```powershell
mvn spring-boot:run
```

后端默认地址：`http://localhost:8080`。

也可以使用 IDEA 运行 `com.cdvtc.demo.SystemApplication`。

## 前端运行

前端已经有 `package.json`，**不需要执行 `npm init`**。在前端目录执行：

```powershell
cd frontend
npm install
npm run dev
```

前端默认地址：`http://localhost:5173`。

Vite 已配置 `/api` 代理到 `http://localhost:8080`。如果后端端口发生变化，请修改 `frontend/vite.config.js`。

前端生产构建：

```powershell
cd frontend
npm run build
```

## 后端打包

```powershell
cd backend
mvn clean package
java -jar target/user-management-1.0.0.jar
```

`target/`、`node_modules/`、`frontend/dist/`、IDE 配置和本地配置文件不会提交到远端。

## Git 推送前检查

```powershell
git status --short
git status --ignored --short
```

确认以下内容没有被加入提交：

- `backend/src/main/resources/application.yml`
- `backend/target/`
- `frontend/node_modules/`
- `frontend/dist/`
- `backend/.idea/`
- 本地日志和临时文件

## 说明

项目不接入 Flyway。数据库脚本按 `V0`、`V1`、`V2` 的项目约定手工按顺序执行。真实支付、改签、站点停用/恢复和批量地区车站维护不在当前课设范围内。
