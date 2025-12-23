# Xudis Auth Center - 测试客户端实现说明

## 概述

本项目实现了一个完整的测试客户端应用，用于测试和演示 **Xudis Auth Center** 的所有认证和授权功能。

## 项目结构

```
xudis_auth_center/
├── src/                          # 认证中心主项目
├── test-client/                  # 测试客户端 (新增)
│   ├── pom.xml
│   ├── README.md
│   ├── start-client.sh
│   └── src/
│       └── main/
│           ├── java/
│           │   └── com/xudis/authclient/
│           │       ├── AuthClientApplication.java
│           │       ├── config/
│           │       │   └── SecurityConfig.java
│           │       ├── controller/
│           │       │   └── TestController.java
│           │       ├── dto/
│           │       │   ├── LoginRequest.java
│           │       │   ├── LoginResponse.java
│           │       │   └── RefreshRequest.java
│           │       └── service/
│           │           └── AuthCenterService.java
│           └── resources/
│               ├── application.yml
│               ├── static/css/
│               │   └── style.css
│               └── templates/
│                   ├── index.html
│                   ├── oauth2-success.html
│                   ├── test-custom-api.html
│                   ├── test-discovery.html
│                   ├── test-jwks.html
│                   └── test-client-credentials.html
├── test-client-build-test.sh     # 构建测试脚本 (新增)
└── README.md                     # 主文档 (已更新)
```

## 功能实现

### 1. OAuth2 Authorization Code + PKCE 流程

- **实现位置**: `SecurityConfig.java`, `TestController.java`
- **功能说明**: 
  - 完整实现标准 OAuth2 授权码流程
  - 自动处理 PKCE (Proof Key for Code Exchange)
  - Spring Security OAuth2 Client 自动处理 token 交换
  - 获取和展示 ID Token、Access Token 和用户信息

- **测试路径**: 
  1. 访问 `http://localhost:8080`
  2. 点击 "开始 OAuth2 登录"
  3. 跳转到认证中心登录
  4. 授权后返回客户端查看结果

### 2. 自定义登录 API 测试

- **实现位置**: `AuthCenterService.java`, `TestController.java`, `test-custom-api.html`
- **功能说明**:
  - 测试用户名/密码登录接口
  - 测试 Token 刷新接口
  - 测试注销接口
  - 解析和展示 JWT Claims

- **API 端点**:
  - `POST /api/auth/login` - 用户登录
  - `POST /api/auth/refresh` - 刷新 Token
  - `POST /api/auth/logout` - 用户注销

### 3. OIDC Discovery 测试

- **实现位置**: `AuthCenterService.java`, `TestController.java`, `test-discovery.html`
- **功能说明**: 
  - 获取 OpenID Connect Provider 元数据
  - 展示所有可用端点和配置信息

- **端点**: `GET /.well-known/openid-configuration`

### 4. JWKS 公钥测试

- **实现位置**: `AuthCenterService.java`, `TestController.java`, `test-jwks.html`
- **功能说明**: 
  - 获取 JWT 验证公钥
  - 展示密钥信息

- **端点**: `GET /oauth2/jwks`

### 5. Client Credentials 流程测试

- **实现位置**: `AuthCenterService.java`, `TestController.java`, `test-client-credentials.html`
- **功能说明**: 
  - 测试机器对机器 (M2M) 认证
  - 使用客户端凭证获取 Access Token
  - 解析和展示 Token Claims

- **端点**: `POST /oauth2/token` (grant_type=client_credentials)

## 技术实现

### 核心技术栈

1. **Spring Boot 3.2.0** - 应用框架
2. **Spring Security 6.x** - 安全框架
3. **Spring OAuth2 Client** - OAuth2 客户端支持
4. **Thymeleaf** - 模板引擎
5. **WebFlux WebClient** - HTTP 客户端（用于测试 API）
6. **Lombok** - 简化代码

### 安全配置

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        // 配置 OAuth2 Login
        // 配置访问权限
        // 配置注销
    }
}
```

### OAuth2 客户端配置

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          xudis-auth:
            client-id: demo-client
            client-secret: demo-secret
            authorization-grant-type: authorization_code
            scope: openid,profile,email,read,write
        provider:
          xudis-auth:
            issuer-uri: http://localhost:9000
```

### Web UI 设计

- **响应式设计**: 支持桌面和移动设备
- **卡片式布局**: 清晰的功能分类
- **渐变配色**: 美观的视觉效果
- **实时反馈**: 显示请求和响应详情
- **中英文混合**: 界面友好

## 使用场景

### 开发和测试

1. **功能验证**: 验证认证中心各项功能是否正常工作
2. **集成测试**: 模拟真实客户端与认证中心的交互
3. **调试工具**: 查看 Token 内容和 API 响应
4. **学习示例**: 演示如何集成 OAuth2/OIDC 认证

### 演示和文档

1. **功能演示**: 向他人展示认证中心功能
2. **API 文档**: 提供实际的 API 调用示例
3. **集成指南**: 为其他开发者提供集成参考

## 启动和使用

### 1. 启动认证中心

```bash
# 启动数据库和 Redis
docker compose up -d

# 启动认证中心
mvn spring-boot:run
```

确认认证中心在 `http://localhost:9000` 运行。

### 2. 启动测试客户端

```bash
# 使用启动脚本
cd test-client
./start-client.sh

# 或手动启动
cd test-client
mvn spring-boot:run
```

客户端将在 `http://localhost:8080` 运行。

### 3. 访问和测试

在浏览器中访问 `http://localhost:8080`，选择要测试的功能。

## 测试凭证

### 用户账号
- **用户名**: `admin`
- **密码**: `admin123!`
- **角色**: `ROLE_ADMIN`, `ROLE_USER`

### OAuth2 客户端
- **Client ID**: `demo-client`
- **Client Secret**: `demo-secret`
- **支持的授权模式**: Authorization Code + PKCE, Client Credentials, Refresh Token
- **Redirect URIs**: 
  - `http://localhost:8080/login/oauth2/code/xudis-auth`
  - `http://localhost:8080/authorized`
- **Scopes**: `openid`, `profile`, `email`, `read`, `write`

## 扩展和自定义

### 添加新的测试功能

1. 在 `AuthCenterService` 中添加新的服务方法
2. 在 `TestController` 中添加新的控制器方法
3. 创建新的 Thymeleaf 模板
4. 在 `index.html` 中添加新的功能卡片

### 修改样式

编辑 `src/main/resources/static/css/style.css` 文件。

### 更改认证中心地址

修改 `application.yml` 中的配置：

```yaml
auth:
  center:
    base-url: http://your-auth-center:port

spring:
  security:
    oauth2:
      client:
        provider:
          xudis-auth:
            issuer-uri: http://your-auth-center:port
```

## 文件说明

### 新增文件

1. **test-client/** - 测试客户端完整项目
2. **test-client-build-test.sh** - 构建验证脚本

### 修改文件

1. **README.md** - 添加了测试客户端章节

## 验证和测试

运行构建测试验证项目正确性：

```bash
./test-client-build-test.sh
```

该脚本将：
1. 编译 Java 代码
2. 打包成 JAR 文件
3. 验证构建成功

## 注意事项

1. **端口冲突**: 确保 8080 端口未被占用
2. **认证中心依赖**: 必须先启动认证中心（及其依赖的 MySQL 和 Redis）
3. **浏览器兼容性**: 建议使用现代浏览器（Chrome, Firefox, Safari, Edge）
4. **CORS 配置**: 认证中心的 CORS 配置已包含 `http://localhost:8080`

## 未来改进

- [ ] 添加 Token 刷新流程的可视化
- [ ] 支持多个客户端配置切换
- [ ] 添加性能测试功能
- [ ] 导出测试报告
- [ ] 支持自定义测试场景

## 参考资料

- [OAuth 2.0 RFC 6749](https://tools.ietf.org/html/rfc6749)
- [OpenID Connect Core 1.0](https://openid.net/specs/openid-connect-core-1_0.html)
- [PKCE RFC 7636](https://tools.ietf.org/html/rfc7636)
- [Spring Security OAuth2 Client](https://docs.spring.io/spring-security/reference/servlet/oauth2/client/index.html)
- [Spring Authorization Server](https://docs.spring.io/spring-authorization-server/docs/current/reference/html/index.html)

## 许可证

MIT License
