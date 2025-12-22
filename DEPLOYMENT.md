# Deployment Guide

## 部署指南

### 开发环境

开发环境已配置完成，直接使用：

```bash
# 1. 启动基础设施
docker compose up -d

# 2. 构建并运行
mvn spring-boot:run
```

### 生产环境部署

#### 1. 环境变量配置

创建 `.env` 文件或配置环境变量：

```bash
# 数据库配置
DB_URL=jdbc:mysql://your-production-db:3306/auth_center
DB_USERNAME=authuser
DB_PASSWORD=<strong-password>

# Redis 配置
REDIS_HOST=your-production-redis
REDIS_PORT=6379
REDIS_PASSWORD=<strong-password>

# JWT 配置
JWT_ISSUER=https://your-domain.com
JWT_TOKEN_VALIDITY=3600
JWT_REFRESH_TOKEN_VALIDITY=86400

# CORS 配置
CORS_ALLOWED_ORIGINS=https://your-frontend.com,https://your-app.com
```

#### 2. RSA 密钥持久化

**重要**: 生产环境必须使用持久化的 RSA 密钥，否则每次重启都会使所有 token 失效。

选项 A - 使用密钥文件:

```java
@Value("${auth.jwt.private-key-location}")
private String privateKeyLocation;

@Value("${auth.jwt.public-key-location}")
private String publicKeyLocation;

// 从文件加载密钥而不是生成新的
```

选项 B - 使用密钥管理服务 (推荐):
- AWS KMS
- Azure Key Vault
- HashiCorp Vault

#### 3. 数据库迁移

生产环境部署前：

```sql
-- 1. 删除或修改默认 admin 用户
DELETE FROM authorities WHERE user_id = (SELECT id FROM users WHERE username = 'admin');
DELETE FROM users WHERE username = 'admin';

-- 2. 创建生产用户
INSERT INTO users (username, password, enabled) VALUES 
('your-admin', '<bcrypt-hashed-password>', TRUE);
```

#### 4. SSL/TLS 配置

在 `application-prod.yml` 中:

```yaml
server:
  port: 8443
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
```

#### 5. Docker 部署

创建 `Dockerfile`:

```dockerfile
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/xudis-auth-center-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 9000
ENTRYPOINT ["java", "-jar", "app.jar"]
```

构建和运行:

```bash
docker build -t xudis-auth-center:latest .
docker run -d \
  --name auth-center \
  -p 9000:9000 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_URL=${DB_URL} \
  -e DB_USERNAME=${DB_USERNAME} \
  -e DB_PASSWORD=${DB_PASSWORD} \
  xudis-auth-center:latest
```

#### 6. Kubernetes 部署

示例 deployment:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: xudis-auth-center
spec:
  replicas: 2
  selector:
    matchLabels:
      app: auth-center
  template:
    metadata:
      labels:
        app: auth-center
    spec:
      containers:
      - name: auth-center
        image: xudis-auth-center:latest
        ports:
        - containerPort: 9000
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: DB_URL
          valueFrom:
            secretKeyRef:
              name: auth-secrets
              key: db-url
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
```

### 监控和日志

建议配置：

1. **应用监控**: Spring Boot Actuator + Prometheus
2. **日志聚合**: ELK Stack 或 Loki
3. **告警**: Grafana + AlertManager

### 性能优化

1. **连接池配置**:
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
```

2. **Redis 连接池**:
```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
```

3. **JVM 参数**:
```bash
java -Xms512m -Xmx1g -XX:+UseG1GC -jar app.jar
```

### 备份策略

1. **数据库备份**: 每日全量 + 增量
2. **Redis 持久化**: RDB + AOF
3. **密钥备份**: 安全存储 RSA 密钥副本

### 安全检查清单

- [ ] 修改所有默认密码
- [ ] 启用 HTTPS/TLS
- [ ] 配置持久化 RSA 密钥
- [ ] 限制 CORS 源
- [ ] 配置防火墙规则
- [ ] 启用速率限制
- [ ] 配置审计日志
- [ ] 定期更新依赖
- [ ] 实施密钥轮换策略
- [ ] 配置备份和恢复

