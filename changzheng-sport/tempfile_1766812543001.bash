# 创建证书文件（把下面的内容替换为你复制的证书）
cat > /opt/changzheng/docker/nginx/ssl/fullchain.pem << 'EOF'
-----BEGIN CERTIFICATE-----
这里粘贴证书内容
-----END CERTIFICATE-----
EOF

# 创建私钥文件（把下面的内容替换为你复制的私钥）
cat > /opt/changzheng/docker/nginx/ssl/privkey.pem << 'EOF'
-----BEGIN RSA PRIVATE KEY-----
这里粘贴私钥内容
-----END RSA PRIVATE KEY-----
EOF

# 验证
ls -la /opt/changzheng/docker/nginx/ssl/
