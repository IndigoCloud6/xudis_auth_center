#!/bin/bash

# Xudis Auth Test Client Startup Script

set -e

BLUE='\033[0;34m'
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Xudis Auth Test Client${NC}"
echo -e "${BLUE}========================================${NC}"

# Check if auth center is running
echo -e "\n${YELLOW}[1] 检查认证中心状态...${NC}"
if curl -s http://localhost:9000/.well-known/openid-configuration > /dev/null 2>&1; then
    echo -e "${GREEN}✓ 认证中心正在运行 (http://localhost:9000)${NC}"
else
    echo -e "${RED}✗ 认证中心未运行！${NC}"
    echo -e "${YELLOW}请先启动认证中心:${NC}"
    echo -e "  cd .."
    echo -e "  docker compose up -d"
    echo -e "  mvn spring-boot:run"
    exit 1
fi

# Check if port 8080 is available
echo -e "\n${YELLOW}[2] 检查端口 8080...${NC}"
if lsof -Pi :8080 -sTCP:LISTEN -t >/dev/null 2>&1; then
    echo -e "${RED}✗ 端口 8080 已被占用${NC}"
    echo -e "${YELLOW}请停止占用端口的程序或修改 application.yml 中的端口配置${NC}"
    exit 1
else
    echo -e "${GREEN}✓ 端口 8080 可用${NC}"
fi

# Build and run the client
echo -e "\n${YELLOW}[3] 启动测试客户端...${NC}"
echo -e "${BLUE}运行命令: mvn spring-boot:run${NC}\n"

mvn spring-boot:run
