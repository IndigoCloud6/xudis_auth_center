# Xudis Auth Test Client

这是一个用于测试 **Xudis Auth Center** 的客户端应用程序。

## 功能特性

### 支持的测试功能

1. **OAuth2 Authorization Code + PKCE 流程**
   - 标准的 OAuth2 授权码流程
   - 自动处理 PKCE (Proof Key for Code Exchange)
   - 获取 ID Token 和 Access Token

2. **自定义登录 API 测试**
   - 用户名/密码登录 (`POST /api/auth/login`)
   - Token 刷新 (`POST /api/auth/refresh`)
   - 用户注销 (`POST /api/auth/logout`)

3. **OIDC Discovery**
   - 测试 OpenID Connect Discovery 端点
   - 查看 Provider 元数据

4. **JWKS 端点测试**
   - 获取 JWT 验证公钥
   - 查看密钥信息

5. **Client Credentials 流程**
   - 测试机器对机器认证
   - 获取客户端凭证 Token

## 快速开始

### 前置要求

1. **启动 Xudis Auth Center**
   
   在主项目目录运行：
   ```bash
   # 启动数据库和 Redis
   docker compose up -d
   
   # 启动认证中心
   mvn spring-boot:run
   ```
   
   确认认证中心在 `http://localhost:9000` 运行。

2. **Java 17+** 和 **Maven 3.6+**

### 启动测试客户端

```bash
# 进入 test-client 目录
cd test-client

# 构建项目
mvn clean package

# 运行客户端
mvn spring-boot:run
```

客户端将在 `http://localhost:8080` 启动。

### 使用测试客户端

1. 在浏览器中访问: `http://localhost:8080`

2. 选择要测试的功能：

   - **OAuth2 授权码流程**: 点击 "开始 OAuth2 登录" 按钮
     - 将跳转到认证中心登录页面
     - 使用默认账号: `admin` / `admin123!`
     - 登录成功后会重定向回客户端，显示用户信息和 tokens

   - **自定义登录 API**: 点击 "测试登录 API" 按钮
     - 输入用户名和密码（默认：admin / admin123!）
     - 查看返回的 Access Token 和 Refresh Token
     - 可以继续测试刷新和注销功能

   - **OIDC Discovery**: 点击 "测试 Discovery" 按钮
     - 查看 OpenID Connect 配置信息

   - **JWKS**: 点击 "测试 JWKS" 按钮
     - 查看 JWT 验证公钥

   - **Client Credentials**: 点击 "测试 Client Credentials" 按钮
     - 输入客户端 ID 和密钥（默认：demo-client / demo-secret）
     - 获取客户端凭证 Token

## 配置说明

配置文件: `src/main/resources/application.yml`

### 主要配置项

```yaml
server:
  port: 8080  # 客户端端口

auth:
  center:
    base-url: http://localhost:9000  # 认证中心地址

spring:
  security:
    oauth2:
      client:
        registration:
          xudis-auth:
            client-id: demo-client
            client-secret: demo-secret
            scope: openid,profile,email,read,write
```

### 修改认证中心地址

如果认证中心运行在其他地址，修改以下配置：

```yaml
auth:
  center:
    base-url: http://your-auth-server:port

spring:
  security:
    oauth2:
      client:
        provider:
          xudis-auth:
            issuer-uri: http://your-auth-server:port
```

## 项目结构

```
test-client/
├── pom.xml                          # Maven 配置
├── README.md                        # 本文档
└── src/
    └── main/
        ├── java/com/xudis/authclient/
        │   ├── AuthClientApplication.java      # 主应用类
        │   ├── config/
        │   │   └── SecurityConfig.java         # 安全配置
        │   ├── controller/
        │   │   └── TestController.java         # 测试控制器
        │   ├── dto/
        │   │   ├── LoginRequest.java           # 登录请求 DTO
        │   │   ├── LoginResponse.java          # 登录响应 DTO
        │   │   └── RefreshRequest.java         # 刷新请求 DTO
        │   └── service/
        │       └── AuthCenterService.java      # 认证中心服务
        └── resources/
            ├── application.yml                 # 应用配置
            ├── static/
            │   └── css/
            │       └── style.css              # 样式文件
            └── templates/                     # Thymeleaf 模板
                ├── index.html                 # 首页
                ├── oauth2-success.html        # OAuth2 登录成功页
                ├── test-custom-api.html       # 自定义 API 测试页
                ├── test-discovery.html        # Discovery 测试页
                ├── test-jwks.html             # JWKS 测试页
                └── test-client-credentials.html  # Client Credentials 测试页
```

## 技术栈

- **Spring Boot 3.2.0** - 应用框架
- **Spring Security 6.x** - 安全框架
- **Spring OAuth2 Client** - OAuth2 客户端支持
- **Thymeleaf** - 模板引擎
- **WebFlux** - 响应式 HTTP 客户端
- **Lombok** - 简化代码

## API 测试说明

### 1. OAuth2 Authorization Code Flow

测试标准的 OAuth2 授权码流程 + PKCE：

1. 点击 "开始 OAuth2 登录"
2. 跳转到认证中心登录页面
3. 输入 `admin` / `admin123!`
4. 授权后重定向回客户端
5. 查看 ID Token、Access Token 和用户信息

### 2. 自定义登录 API

测试自定义的认证 API：

**登录:**
```bash
POST http://localhost:9000/api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123!"
}
```

**刷新:**
```bash
POST http://localhost:9000/api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "{refresh_token}"
}
```

**注销:**
```bash
POST http://localhost:9000/api/auth/logout
Authorization: Bearer {access_token}
```

### 3. Client Credentials Flow

测试机器对机器认证：

```bash
POST http://localhost:9000/oauth2/token
Content-Type: application/x-www-form-urlencoded
Authorization: Basic {base64(client_id:client_secret)}

grant_type=client_credentials&scope=read write
```

## 故障排查

### 无法连接到认证中心

**错误**: Connection refused

**解决方案**:
1. 确认认证中心正在运行: `http://localhost:9000`
2. 检查认证中心的 MySQL 和 Redis 是否启动
3. 查看认证中心日志

### OAuth2 登录失败

**错误**: Invalid redirect_uri

**解决方案**:
1. 确认认证中心的 `demo-client` 配置包含正确的 redirect_uri
2. 默认应该包含: `http://localhost:8080/login/oauth2/code/xudis-auth`

### Token 验证失败

**错误**: Invalid token

**可能原因**:
1. Token 已过期
2. 认证中心重启导致密钥改变
3. Token 已被注销（在黑名单中）

## 默认凭证

### 用户账号
- **用户名**: `admin`
- **密码**: `admin123!`

### OAuth2 客户端
- **Client ID**: `demo-client`
- **Client Secret**: `demo-secret`
- **Redirect URI**: `http://localhost:8080/login/oauth2/code/xudis-auth`
- **Scopes**: `openid`, `profile`, `email`, `read`, `write`

## 开发说明

### 添加新的测试功能

1. 在 `AuthCenterService` 中添加新的服务方法
2. 在 `TestController` 中添加新的控制器方法
3. 创建相应的 Thymeleaf 模板
4. 在首页添加新的测试卡片

### 自定义样式

修改 `src/main/resources/static/css/style.css` 文件。

## 相关链接

- 认证中心地址: http://localhost:9000
- 客户端地址: http://localhost:8080
- OIDC Discovery: http://localhost:9000/.well-known/openid-configuration
- JWKS: http://localhost:9000/oauth2/jwks

## 许可证

MIT License
