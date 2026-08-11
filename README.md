# 云上重走长征路

一个将微信运动步数映射到虚拟长征路线的高校主题教育项目。学生可以同步运动数据、解锁历史节点和学习内容，并查看个人、班级与年级排行；维护人员通过 Vue 管理端维护学生和内容数据。

## 主要组件

- changzheng-gateway：统一 API 入口、JWT 校验与角色隔离。
- changzheng-auth：微信登录、学生绑定、用户资料和令牌刷新。
- changzheng-sport：微信运动数据解密、步数同步与里程计算。
- changzheng-content：路线节点、学习记录及图片/音视频上传。
- changzheng-rank：个人、班级和年级排行。
- changzheng-admin / changzheng-admin-web：管理 API 与 Vue 3 管理端。
- changzheng-miniprogram：原生微信小程序客户端。

后端使用 Java 17、Spring Boot 3.2、Spring Cloud Gateway、MySQL、Redis、Nacos 和 RocketMQ；管理端使用 Vue 3、Vite 和 Element Plus。

## 本地验证

要求 JDK 17、Maven 3.8+、Node.js 20.19+（或 22.12+）和 Docker Compose。

    mvn --batch-mode verify
    npm --prefix changzheng-admin-web ci
    npm --prefix changzheng-admin-web run build
    docker compose --env-file .env.example config --quiet

管理端开发服务器默认把 /api 代理到网关 http://localhost:8080。如需使用前端模拟登录，请显式设置 VITE_USE_MOCK=true；默认会连接真实后端。

## 配置与启动

复制环境变量模板并替换每个占位值：

    cp .env.example .env

关键要求：

- JWT_SECRET 至少 32 字节。
- AES_KEY 必须恰好为 16、24 或 32 字节；更换它会影响既有加密数据的读取。
- MySQL root 密码、应用密码和 Redis 密码应各不相同。
- ADMIN_PASSWORD 至少 12 个字符，只在数据库不存在启用中的管理员时创建初始管理员。
- CORS_ALLOWED_ORIGIN 必须是管理端的准确 Origin，不支持通配符。
- FILE_UPLOAD_URL 应指向对外可访问的 /uploads 地址。

准备好 .env 后运行：

    ./deploy.sh

脚本会先验证配置、运行后端测试、构建管理端，再构建并启动 Compose 服务。默认只向宿主机公开 Nginx 的 80 端口；MySQL、Redis、Nacos、RocketMQ 和内部微服务仅位于 Compose 网络。生产 HTTPS 应在可信的外部反向代理或负载均衡器终止，并将流量转发到该入口。

## 数据库迁移

全新 MySQL 数据卷会按顺序运行 sql/ 中的初始化脚本。对于已有部署，请先备份数据库，再由维护人员审核并手动执行尚未应用的迁移。V3__security_hardening.sql 会修正微信用户未绑定前的可空字段，并禁用仍使用历史公开密码哈希的默认管理员。

## 安全说明

网关和每个 Servlet 微服务都会验证 JWT，并从已验证 claims 重建内部身份头。管理员上传接口与学生业务接口使用不同角色；refresh token 不能调用普通 API。上传同时校验扩展名和文件签名，删除操作限制在上传根目录内。

不要提交 .env、微信凭证、JWT、数据库导出、学生数据、上传内容、node_modules 或 dist。安全问题请按 [SECURITY.md](SECURITY.md) 私下报告，贡献流程见 [CONTRIBUTING.md](CONTRIBUTING.md)。
