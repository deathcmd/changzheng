<div align="center">

# 云上重走长征路

将微信运动数据映射到虚拟长征路线的高校主题教育与运动学习平台。

[![CI](https://github.com/deathcmd/changzheng/actions/workflows/ci.yml/badge.svg)](https://github.com/deathcmd/changzheng/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
![Java 17](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot 3.5](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=springboot&logoColor=white)
![Vue 3](https://img.shields.io/badge/Vue-3-4FC08D?logo=vuedotjs&logoColor=white)
![WeChat Mini Program](https://img.shields.io/badge/WeChat-Mini%20Program-07C160?logo=wechat&logoColor=white)

[核心能力](#核心能力) · [系统架构](#系统架构) · [快速开始](#快速开始) · [安全设计](#安全设计) · [参与贡献](#参与贡献)

</div>

## 项目简介

“云上重走长征路”面向高校学生与活动组织者，把用户授权的微信运动步数折算为虚拟长征里程。学生在运动过程中逐步解锁历史节点与图文、音视频学习内容，并可查看总榜和同年级排行；维护人员通过独立管理端查看活动统计并维护学生数据。

项目由原生微信小程序、Vue 3 管理端和一组 Spring Cloud 微服务组成。当前实现覆盖学生身份绑定、运动数据同步、路线学习、个人排行、学生管理和活动统计等核心流程。

## 核心能力

- **微信运动同步**：解密用户授权的微信运动数据，记录每日步数并避免重复同步。
- **虚拟路线进度**：按配置规则将步数折算为里程，展示累计进度和已经到达的路线节点。
- **节点内容学习**：到达指定里程后解锁历史节点及其图文、音频和视频内容，并记录学习状态。
- **排行榜**：提供个人总榜、同年级榜和当前用户排名。
- **学生身份绑定**：通过微信登录并绑定学号等信息，为用户数据建立明确归属。
- **运营管理后台**：支持管理员登录、学生数据导入与维护、节点及学习内容 CRUD、文件上传和仪表盘统计。

## 系统架构

```mermaid
flowchart LR
    Mini["微信小程序"] --> Entry["Nginx 统一入口"]
    AdminWeb["Vue 3 管理端"] --> Entry
    Entry --> Gateway["Spring Cloud Gateway"]

    Gateway --> Auth["认证服务"]
    Gateway --> Sport["运动服务"]
    Gateway --> Content["内容服务"]
    Gateway --> Rank["排行服务"]
    Gateway --> Admin["管理服务"]

    Auth --> MySQL[("MySQL")]
    Sport --> MySQL
    Content --> MySQL
    Rank --> MySQL
    Admin --> MySQL
    Gateway --> Redis[("Redis")]
    Gateway -. "服务发现" .-> Nacos["Nacos"]
    Auth -.-> Nacos
    Sport -.-> Nacos
    Content -.-> Nacos
    Rank -.-> Nacos
    Admin -.-> Nacos
```

外部请求统一通过 Nginx 与网关进入系统。网关完成路由和第一层身份校验，各业务服务再次验证令牌、账号状态并执行角色授权；MySQL 保存权威业务数据，Redis 和 Nacos 分别承担限流缓存与服务发现。

## 技术栈

| 层级 | 主要技术 |
| --- | --- |
| 小程序 | 原生微信小程序 |
| 管理端 | Vue 3、Vite、Element Plus、ECharts |
| 服务端 | Java 17、Spring Boot 3.5、Spring Cloud 2025.0、MyBatis-Plus |
| 基础设施 | MySQL 8、Redis 7、Nacos 2.3 |
| 部署与质量 | Docker Compose、Nginx、Maven Wrapper、GitHub Actions、CodeQL、Dependabot |

## 项目结构

| 目录 | 职责 |
| --- | --- |
| `changzheng-gateway` | 统一 API 入口、路由、JWT 校验与角色隔离 |
| `changzheng-auth` | 微信登录、学生绑定、用户资料与令牌刷新 |
| `changzheng-sport` | 微信运动数据解密、步数同步与里程计算 |
| `changzheng-content` | 路线节点、学习记录与图片/音视频文件管理 |
| `changzheng-rank` | 个人总榜、同年级榜与当前用户排名 |
| `changzheng-admin` | 管理员认证、学生导入与维护、仪表盘统计 API |
| `changzheng-admin-web` | Vue 3 管理端 |
| `changzheng-miniprogram` | 原生微信小程序客户端 |
| `changzheng-common` | 公共实体、响应模型、工具类与安全过滤器 |
| `sql` / `docker` | 数据库初始化与迁移、镜像和 Nginx 配置 |

## 快速开始

### 环境要求

- JDK 17
- Node.js 20.19+ 或 22.12+
- Docker 与 Docker Compose

### 验证源码

```bash
./mvnw --batch-mode --no-transfer-progress clean verify
npm --prefix changzheng-admin-web ci --ignore-scripts
npm --prefix changzheng-admin-web run build
npm --prefix changzheng-admin-web audit --audit-level=high
docker compose --env-file .env.example config --quiet
```

Windows 使用 `.\mvnw.cmd --batch-mode --no-transfer-progress clean verify`。Wrapper 固定并校验 Maven 3.9.11，首次运行会从 Maven Central 下载发行包。

管理端开发服务器默认将 `/api` 代理到 `http://localhost:8080`。如需使用前端模拟登录，请显式设置 `VITE_USE_MOCK=true`；默认连接真实后端。

### 配置与部署

复制环境变量模板，并替换其中的每个占位值：

```bash
cp .env.example .env
```

配置时请确保：

- `JWT_SECRET` 至少为 32 字节。
- `AES_KEY` 恰好为 16、24 或 32 字节；更换后会影响既有加密数据的读取。
- MySQL root 密码、应用密码和 Redis 密码各不相同。
- `ADMIN_PASSWORD` 至少为 12 个字符；仅在没有启用中的管理员时用于创建初始管理员。
- `CORS_ALLOWED_ORIGIN` 是管理端准确的 Origin，不使用通配符。
- `FILE_UPLOAD_URL` 指向外部可访问的 `/uploads` 地址。
- `TOTAL_MARCH_DISTANCE` 在运动进度与管理仪表盘中使用同一数值。

然后执行：

```bash
./deploy.sh
```

部署脚本会验证配置、运行后端测试、构建管理端，再构建并启动 Compose 服务。默认仅向宿主机公开 Nginx 的 `80` 端口，MySQL、Redis、Nacos 和内部微服务只在 Compose 网络内通信。生产环境应在可信的外部反向代理或负载均衡器终止 HTTPS。

部署完成后的主要入口：

- `/admin/`：管理端页面
- `/api/`：学生端与管理端 API
- `/uploads/`：受控上传文件的公开读取路径

更完整的环境准备、微信平台配置和部署步骤见 [部署文档](部署文档.md)，业务操作说明见 [使用文档](使用文档.md)。

## 数据库迁移

全新的 MySQL 数据卷会按文件名顺序执行 `sql/` 中的初始化脚本。已有部署应先备份数据库，再由维护人员审核并按版本顺序手动执行尚未应用的迁移。V3 修正绑定前字段并禁用历史默认管理员；V4 拆分学院与专业、将里程流水改为追加式审计记录、补齐节点内容字段和学习记录表，并把历史文章标记转换为纯文本。

## 当前实现边界

节点与内容管理、学生模板本地生成、公开系统配置和用户信息接口已经接通。当前轮播图接口返回空列表；成就卡片由小程序基于进度本地计算，尚无独立的服务端成就 API；当前也没有班级汇总排行榜。完整边界见 [使用手册](使用文档.md#4-当前实现边界)，后续计划见 [路线图](ROADMAP.md)。

## 安全设计

- 网关和各 Servlet 微服务独立验证 JWT；受保护接口还会核验账号启用状态，使停用操作立即撤销旧访问令牌。
- 内部用户与管理员身份头由已验证的 claims 重建，不信任客户端传入值。
- 学生、管理员和 refresh token 使用不同角色与用途约束。
- 管理员登录失败计数在 Redis 中原子更新，15 分钟内最多尝试 10 次。
- 节点学习内容在列表、详情和学习记录入口均校验用户已解锁对应节点；文章以纯文本渲染。
- 上传文件同时校验扩展名和文件签名；删除操作限制在上传根目录内，并阻止路径穿越。
- 部署要求显式提供密钥和密码，Java 容器以非 root 用户运行，内部服务默认不暴露到宿主机。
- 管理端使用锁文件安装依赖并禁用依赖生命周期脚本；CI 验证后端、管理端依赖审计与构建、小程序 JavaScript 和 Compose 配置。CodeQL 分析 Java 与 JavaScript，Gitleaks 扫描 Git 历史凭证，Dependabot 监控 Maven、npm、Actions 和容器镜像。

请勿提交 `.env`、微信凭证、JWT、私钥、数据库导出、学生数据、上传内容、`node_modules` 或 `dist`。发现漏洞时不要创建公开 Issue，请按照 [安全策略](SECURITY.md) 私下报告。

## 参与贡献

欢迎通过 Issue 报告可复现的问题或提出范围明确的改进建议。提交 Pull Request 前，请阅读 [贡献指南](CONTRIBUTING.md)，保持改动聚焦，并确保相关测试和构建通过。

有用的维护文档：

- [部署文档](部署文档.md)
- [使用文档](使用文档.md)
- [贡献指南](CONTRIBUTING.md)
- [安全策略](SECURITY.md)
- [路线图](ROADMAP.md)

## 开源许可证

本项目采用 [Apache License 2.0](LICENSE) 开源。使用、修改和分发时请遵守许可证条款，并保留 [NOTICE](NOTICE) 中的版权声明。
