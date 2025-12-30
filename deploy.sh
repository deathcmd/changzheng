#!/bin/bash
# 云上重走长征路 - Ubuntu服务器一键部署脚本
# 使用方法: chmod +x deploy.sh && sudo ./deploy.sh

set -e

echo "=========================================="
echo "  云上重走长征路 - 服务器部署"
echo "  四川工商职业技术学院 智能制造与信息工程学院"
echo "=========================================="

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 配置变量（请根据实际情况修改）
MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-changzheng123}"
MYSQL_DATABASE="changzheng_db"
JWT_SECRET="${JWT_SECRET:-changzheng-cloud-march-secret-key-2024-very-long}"
WX_APPID="${WX_APPID:-your_appid}"
WX_SECRET="${WX_SECRET:-your_secret}"

# 项目路径
PROJECT_DIR="/opt/changzheng"
JAVA_VERSION="17"

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# 检查root权限
check_root() {
    if [ "$EUID" -ne 0 ]; then
        log_error "请使用 sudo 运行此脚本"
        exit 1
    fi
}

# 安装基础依赖
install_dependencies() {
    log_info "更新系统包..."
    apt-get update -y
    
    log_info "安装基础依赖..."
    apt-get install -y curl wget git unzip software-properties-common apt-transport-https ca-certificates gnupg lsb-release
}

# 安装JDK 17
install_java() {
    if java -version 2>&1 | grep -q "17"; then
        log_info "JDK 17 已安装"
        return
    fi
    
    log_info "安装 OpenJDK 17..."
    apt-get install -y openjdk-17-jdk
    
    echo "export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64" >> /etc/profile.d/java.sh
    echo "export PATH=\$PATH:\$JAVA_HOME/bin" >> /etc/profile.d/java.sh
    source /etc/profile.d/java.sh
    
    log_info "Java版本: $(java -version 2>&1 | head -n 1)"
}

# 安装Maven
install_maven() {
    if command -v mvn &> /dev/null; then
        log_info "Maven 已安装"
        return
    fi
    
    log_info "安装 Maven..."
    apt-get install -y maven
    log_info "Maven版本: $(mvn -version | head -n 1)"
}

# 安装MySQL 8
install_mysql() {
    if command -v mysql &> /dev/null; then
        log_info "MySQL 已安装"
        return
    fi
    
    log_info "安装 MySQL 8..."
    apt-get install -y mysql-server
    
    systemctl start mysql
    systemctl enable mysql
    
    log_info "配置MySQL..."
    mysql -e "ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '${MYSQL_ROOT_PASSWORD}';"
    mysql -u root -p${MYSQL_ROOT_PASSWORD} -e "CREATE DATABASE IF NOT EXISTS ${MYSQL_DATABASE} DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    mysql -u root -p${MYSQL_ROOT_PASSWORD} -e "FLUSH PRIVILEGES;"
    
    log_info "MySQL安装完成"
}

# 安装Redis
install_redis() {
    if command -v redis-server &> /dev/null; then
        log_info "Redis 已安装"
        return
    fi
    
    log_info "安装 Redis..."
    apt-get install -y redis-server
    
    systemctl start redis-server
    systemctl enable redis-server
    
    log_info "Redis安装完成"
}

# 安装Node.js
install_nodejs() {
    if command -v node &> /dev/null; then
        log_info "Node.js 已安装: $(node -v)"
        return
    fi
    
    log_info "安装 Node.js 18..."
    curl -fsSL https://deb.nodesource.com/setup_18.x | bash -
    apt-get install -y nodejs
    
    log_info "Node.js版本: $(node -v)"
}

# 安装Nginx
install_nginx() {
    if command -v nginx &> /dev/null; then
        log_info "Nginx 已安装"
        return
    fi
    
    log_info "安装 Nginx..."
    apt-get install -y nginx
    
    systemctl start nginx
    systemctl enable nginx
    
    log_info "Nginx安装完成"
}

# 初始化数据库
init_database() {
    log_info "初始化数据库..."
    
    if [ -f "${PROJECT_DIR}/sql/V1__init_schema.sql" ]; then
        mysql -u root -p${MYSQL_ROOT_PASSWORD} ${MYSQL_DATABASE} < "${PROJECT_DIR}/sql/V1__init_schema.sql"
        log_info "数据库表结构初始化完成"
    fi
    
    if [ -f "${PROJECT_DIR}/sql/V2__init_data.sql" ]; then
        mysql -u root -p${MYSQL_ROOT_PASSWORD} ${MYSQL_DATABASE} < "${PROJECT_DIR}/sql/V2__init_data.sql"
        log_info "初始数据导入完成"
    fi
}

# 构建后端项目
build_backend() {
    log_info "构建后端项目..."
    
    cd ${PROJECT_DIR}
    
    # 构建公共模块
    if [ -d "changzheng-common" ]; then
        cd changzheng-common
        mvn clean install -DskipTests
        cd ..
    fi
    
    # 构建认证服务
    if [ -d "changzheng-auth" ]; then
        log_info "构建认证服务..."
        cd changzheng-auth
        mvn clean package -DskipTests
        cd ..
    fi
    
    # 构建管理服务
    if [ -d "changzheng-admin" ]; then
        log_info "构建管理服务..."
        cd changzheng-admin
        mvn clean package -DskipTests
        cd ..
    fi
    
    log_info "后端构建完成"
}

# 构建前端项目
build_frontend() {
    log_info "构建管理后台前端..."
    
    cd ${PROJECT_DIR}/changzheng-admin-web
    npm install
    npm run build
    
    # 复制到Nginx目录
    rm -rf /var/www/changzheng-admin
    cp -r dist /var/www/changzheng-admin
    
    log_info "前端构建完成"
}

# 创建systemd服务
create_services() {
    log_info "创建系统服务..."
    
    # 认证服务
    cat > /etc/systemd/system/changzheng-auth.service << EOF
[Unit]
Description=Changzheng Auth Service
After=network.target mysql.service redis.service

[Service]
Type=simple
User=root
WorkingDirectory=${PROJECT_DIR}/changzheng-auth
Environment="MYSQL_HOST=localhost"
Environment="MYSQL_PORT=3306"
Environment="MYSQL_USER=root"
Environment="MYSQL_PASSWORD=${MYSQL_ROOT_PASSWORD}"
Environment="REDIS_HOST=localhost"
Environment="JWT_SECRET=${JWT_SECRET}"
Environment="WX_APPID=${WX_APPID}"
Environment="WX_SECRET=${WX_SECRET}"
Environment="NACOS_SERVER=localhost:8848"
ExecStart=/usr/bin/java -jar -Xms256m -Xmx512m ${PROJECT_DIR}/changzheng-auth/target/changzheng-auth-1.0.0.jar --spring.profiles.active=prod
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

    # 管理服务
    cat > /etc/systemd/system/changzheng-admin.service << EOF
[Unit]
Description=Changzheng Admin Service
After=network.target mysql.service redis.service

[Service]
Type=simple
User=root
WorkingDirectory=${PROJECT_DIR}/changzheng-admin
Environment="MYSQL_HOST=localhost"
Environment="MYSQL_PORT=3306"
Environment="MYSQL_USER=root"
Environment="MYSQL_PASSWORD=${MYSQL_ROOT_PASSWORD}"
Environment="REDIS_HOST=localhost"
Environment="NACOS_SERVER=localhost:8848"
Environment="SERVER_PORT=8085"
ExecStart=/usr/bin/java -jar -Xms256m -Xmx512m ${PROJECT_DIR}/changzheng-admin/target/changzheng-admin-1.0.0.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

    systemctl daemon-reload
    log_info "系统服务创建完成"
}

# 配置Nginx
configure_nginx() {
    log_info "配置Nginx..."
    
    cat > /etc/nginx/sites-available/changzheng << 'EOF'
# 管理后台
server {
    listen 80;
    server_name localhost;
    
    # 管理后台前端
    location / {
        root /var/www/changzheng-admin;
        index index.html;
        try_files $uri $uri/ /index.html;
    }
    
    # 管理后台API
    location /api/admin/ {
        proxy_pass http://127.0.0.1:8085/admin/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        client_max_body_size 10m;
    }
    
    # 认证API（小程序调用）
    location /api/auth/ {
        proxy_pass http://127.0.0.1:8081/api/auth/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
    
    # 小程序其他API
    location /api/ {
        proxy_pass http://127.0.0.1:8081/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
EOF

    ln -sf /etc/nginx/sites-available/changzheng /etc/nginx/sites-enabled/
    rm -f /etc/nginx/sites-enabled/default
    
    nginx -t && systemctl reload nginx
    log_info "Nginx配置完成"
}

# 启动所有服务
start_services() {
    log_info "启动服务..."
    
    systemctl start changzheng-auth
    systemctl enable changzheng-auth
    sleep 5
    
    systemctl start changzheng-admin
    systemctl enable changzheng-admin
    
    log_info "所有服务已启动"
}

# 显示状态
show_status() {
    echo ""
    echo "=========================================="
    echo "  部署完成！"
    echo "=========================================="
    echo ""
    echo "服务状态:"
    systemctl is-active --quiet changzheng-auth && echo -e "  认证服务: ${GREEN}运行中${NC}" || echo -e "  认证服务: ${RED}未运行${NC}"
    systemctl is-active --quiet changzheng-admin && echo -e "  管理服务: ${GREEN}运行中${NC}" || echo -e "  管理服务: ${RED}未运行${NC}"
    systemctl is-active --quiet nginx && echo -e "  Nginx: ${GREEN}运行中${NC}" || echo -e "  Nginx: ${RED}未运行${NC}"
    systemctl is-active --quiet mysql && echo -e "  MySQL: ${GREEN}运行中${NC}" || echo -e "  MySQL: ${RED}未运行${NC}"
    systemctl is-active --quiet redis-server && echo -e "  Redis: ${GREEN}运行中${NC}" || echo -e "  Redis: ${RED}未运行${NC}"
    echo ""
    echo "访问地址:"
    echo "  管理后台: http://服务器IP/"
    echo "  认证API:  http://服务器IP/api/auth/"
    echo "  管理API:  http://服务器IP/api/admin/"
    echo ""
    echo "常用命令:"
    echo "  查看认证服务日志: journalctl -u changzheng-auth -f"
    echo "  查看管理服务日志: journalctl -u changzheng-admin -f"
    echo "  重启认证服务: systemctl restart changzheng-auth"
    echo "  重启管理服务: systemctl restart changzheng-admin"
    echo ""
    echo "小程序配置:"
    echo "  请将小程序的 API 地址配置为: https://你的域名/api/"
    echo ""
}

# 主函数
main() {
    check_root
    
    # 创建项目目录
    mkdir -p ${PROJECT_DIR}
    
    log_warn "请确保已将项目代码上传到 ${PROJECT_DIR}"
    read -p "项目代码是否已上传？(y/n): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        log_error "请先上传项目代码后再运行此脚本"
        exit 1
    fi
    
    install_dependencies
    install_java
    install_maven
    install_mysql
    install_redis
    install_nodejs
    install_nginx
    init_database
    build_backend
    build_frontend
    create_services
    configure_nginx
    start_services
    show_status
}

main "$@"
