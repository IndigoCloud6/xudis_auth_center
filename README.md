# Xudis Auth Center

一个完整的 OAuth2 + OIDC 认证授权中心，基于 Spring Boot 3.x 和 Spring Authorization Server。

## 功能特性

### A) 标准 OAuth2 + OIDC 支持
- ✅ 完整的 OAuth2 Authorization Server
- ✅ OIDC (OpenID Connect) 支持
- ✅ 自动发现端点：`/.well-known/openid-configuration`
- ✅ 支持授权模式：
  - Authorization Code + PKCE
  - Client Credentials
  - Refresh Token
- ✅ RSA 签名的 JWT (access_token 和 id_token)
- ✅ JWKS 端点：`/oauth2/jwks`
- ✅ JDBC 存储：registered clients、authorization、consent

### B) 自定义登录 API
- ✅ `POST /api/auth/login` - 用户名/密码登录
- ✅ `POST /api/auth/refresh` - 刷新令牌
- ✅ `POST /api/auth/logout` - 注销（Token 黑名单）
- ✅ JWT 使用与 OAuth2 相同的 RSA keypair
- ✅ Redis 存储 refresh_token 和黑名单

### 技术栈
- Spring Boot 3.2.0
- Spring Security 6.x
- Spring Authorization Server 1.2.0
- MySQL 8.0
- Redis 7.2
- Flyway (数据库迁移)
- JWT with RSA签名
- Lombok

## 快速开始

### 前置要求
- JDK 17+
- Maven 3.6+
- Docker & Docker Compose

### 1. 启动基础设施

启动 MySQL 和 Redis：

```bash
docker compose up -d
```

验证服务状态：
```bash
docker compose ps
```

### 2. 构建和运行应用

```bash
# 构建项目
mvn clean package

# 运行应用
mvn spring-boot:run
```

应用将在 `http://localhost:9000` 启动。

### 3. 验证部署

使用自动化测试脚本：
```bash
./test-auth-center.sh
```

或手动访问以下端点验证：
- OIDC Discovery: http://localhost:9000/.well-known/openid-configuration
- JWKS: http://localhost:9000/oauth2/jwks

## 默认凭证

### 用户账号
- **用户名**: `admin`
- **密码**: `admin123!`
- **角色**: `ROLE_ADMIN`, `ROLE_USER`

### OAuth2 客户端
- **Client ID**: `demo-client`
- **Client Secret**: `demo-secret`
- **支持的授权模式**: Authorization Code + PKCE, Client Credentials, Refresh Token
- **Redirect URIs**: 
  - `http://localhost:8080/login/oauth2/code/demo-client`
  - `http://localhost:8080/authorized`
- **Scopes**: `openid`, `profile`, `email`, `read`, `write`

## API 使用示例

### 自定义认证 API

#### 1. 登录获取 Token

```bash
curl -X POST http://localhost:9000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123!"
  }'
```

响应示例：
```json
{
  "accessToken": "eyJraWQiOiI...",
  "refreshToken": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

#### 2. 刷新 Token

```bash
curl -X POST http://localhost:9000/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "f47ac10b-58cc-4372-a567-0e02b2c3d479"
  }'
```

#### 3. 注销

```bash
curl -X POST http://localhost:9000/api/auth/logout \
  -H "Authorization: Bearer eyJraWQiOiI..."
```

### OAuth2 标准流程

#### 1. Client Credentials 模式

获取 client credentials token：

```bash
curl -X POST http://localhost:9000/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -u demo-client:demo-secret \
  -d "grant_type=client_credentials&scope=read write"
```

响应示例：
```json
{
  "access_token": "eyJraWQiOiI...",
  "token_type": "Bearer",
  "expires_in": 3599,
  "scope": "read write"
}
```

#### 2. Authorization Code + PKCE 流程

**步骤 1**: 生成 PKCE 参数

```bash
# 生成 code_verifier (43-128字符的随机字符串)
CODE_VERIFIER=$(openssl rand -base64 64 | tr -d '\n' | tr '+/' '-_' | head -c 43)
echo "Code Verifier: $CODE_VERIFIER"

# 生成 code_challenge (SHA256 hash of code_verifier, base64url encoded)
CODE_CHALLENGE=$(echo -n $CODE_VERIFIER | openssl dgst -sha256 -binary | base64 | tr -d '\n' | tr '+/' '-_' | tr -d '=')
echo "Code Challenge: $CODE_CHALLENGE"
```

**步骤 2**: 在浏览器中访问授权端点

```
http://localhost:9000/oauth2/authorize?
  response_type=code&
  client_id=demo-client&
  redirect_uri=http://localhost:8080/authorized&
  scope=openid profile email&
  code_challenge={CODE_CHALLENGE}&
  code_challenge_method=S256
```

用户登录后会重定向到 `http://localhost:8080/authorized?code={AUTHORIZATION_CODE}`

**步骤 3**: 使用授权码换取 token

```bash
curl -X POST http://localhost:9000/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -u demo-client:demo-secret \
  -d "grant_type=authorization_code" \
  -d "code={AUTHORIZATION_CODE}" \
  -d "redirect_uri=http://localhost:8080/authorized" \
  -d "code_verifier=$CODE_VERIFIER"
```

响应示例：
```json
{
  "access_token": "eyJraWQiOiI...",
  "refresh_token": "...",
  "id_token": "eyJraWQiOiI...",
  "token_type": "Bearer",
  "expires_in": 3599,
  "scope": "openid profile email"
}
```

#### 3. 使用 Refresh Token

```bash
curl -X POST http://localhost:9000/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -u demo-client:demo-secret \
  -d "grant_type=refresh_token" \
  -d "refresh_token={REFRESH_TOKEN}"
```

## 端点说明

### OAuth2 / OIDC 端点
- `GET /.well-known/openid-configuration` - OIDC Discovery
- `GET /.well-known/oauth-authorization-server` - OAuth2 Metadata
- `GET /oauth2/authorize` - 授权端点
- `POST /oauth2/token` - Token 端点
- `GET /oauth2/jwks` - JWKS 公钥端点
- `POST /oauth2/revoke` - Token 撤销端点
- `POST /oauth2/introspect` - Token 内省端点
- `GET /userinfo` - OIDC UserInfo 端点

### 自定义认证端点
- `POST /api/auth/login` - 用户登录
- `POST /api/auth/refresh` - 刷新令牌
- `POST /api/auth/logout` - 用户注销

### 登录页面
- `GET /login` - OAuth2 登录页面

## 配置说明

主要配置文件：`src/main/resources/application.yml`

### 数据库配置
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/auth_center
    username: authuser
    password: authpass
```

### Redis 配置
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: redispass
```

### JWT 配置
```yaml
auth:
  jwt:
    issuer: http://localhost:9000
    token-validity-seconds: 3600
    refresh-token-validity-seconds: 86400
```

### CORS 配置
```yaml
auth:
  cors:
    allowed-origins: http://localhost:3000,http://localhost:8080
    allowed-methods: GET,POST,PUT,DELETE,OPTIONS
```

## 数据库迁移

Flyway 自动执行数据库迁移脚本：
- `V1__Create_user_tables.sql` - 用户表和权限表
- `V2__Create_oauth2_tables.sql` - OAuth2 相关表

## 资源服务器验证

资源服务器可以通过以下方式验证 JWT：

### 方法 1: 使用 JWKS 端点（推荐）

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:9000
          # 或直接指定 jwk-set-uri
          # jwk-set-uri: http://localhost:9000/oauth2/jwks
```

### 方法 2: 本地验证

从 `/oauth2/jwks` 获取公钥并配置本地验证。

## 安全注意事项

1. **生产环境配置**：
   - 修改所有默认密码
   - 使用 HTTPS
   - 配置正确的 issuer URL
   - 限制 CORS 允许的源

2. **密钥管理**：
   - 当前使用运行时生成的 RSA 密钥对
   - 生产环境建议使用持久化的密钥或密钥管理服务

3. **Token 安全**：
   - Access Token 过期时间：1小时
   - Refresh Token 过期时间：24小时
   - 注销的 Token 会被加入 Redis 黑名单

## 开发说明

### 项目结构
```
src/main/java/com/xudis/auth/
├── config/              # 配置类
│   ├── AuthorizationServerConfig.java
│   ├── SecurityConfig.java
│   ├── CorsConfig.java
│   ├── RedisConfig.java
│   └── RegisteredClientConfig.java
├── controller/          # 控制器
│   └── AuthController.java
├── dto/                 # 数据传输对象
│   ├── LoginRequest.java
│   ├── AuthResponse.java
│   └── RefreshRequest.java
├── entity/              # 实体类
│   ├── User.java
│   └── Authority.java
├── repository/          # 数据访问层
│   └── UserRepository.java
├── security/            # 安全相关
│   └── CustomUserDetailsService.java
├── service/             # 业务逻辑
│   ├── AuthService.java
│   └── JwtTokenService.java
└── AuthCenterApplication.java
```

### 添加新用户

在数据库中插入：
```sql
-- 密码需要使用 BCrypt 加密
INSERT INTO users (username, password, enabled) VALUES 
('newuser', '$2a$10$...', TRUE);

INSERT INTO authorities (user_id, authority) VALUES 
((SELECT id FROM users WHERE username = 'newuser'), 'ROLE_USER');
```

### 添加新的 OAuth2 客户端

可以通过 `RegisteredClientRepository` 编程方式添加，或直接插入数据库。

## 故障排查

### 自动化测试
运行 `./test-auth-center.sh` 脚本可快速验证所有功能是否正常工作。该脚本会测试：
- OIDC Discovery
- JWKS 端点
- 自定义登录 API
- Token 刷新
- OAuth2 客户端凭证流程
- 注销功能

### 无法连接数据库
```bash
# 检查 MySQL 容器状态
docker-compose ps

# 查看日志
docker-compose logs mysql
```

### 无法连接 Redis
```bash
# 测试 Redis 连接
docker exec -it xudis-auth-redis redis-cli -a redispass ping
```

### Token 验证失败
- 检查 token 是否过期
- 验证 issuer 配置是否匹配
- 检查 JWKS 端点是否可访问

## 停止服务

```bash
# 停止应用（Ctrl+C）

# 停止并删除容器
docker-compose down

# 停止并删除容器和数据卷
docker-compose down -v
```

## 许可证

MIT License

## 联系方式

如有问题或建议，请提交 Issue。
