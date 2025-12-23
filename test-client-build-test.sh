#!/bin/bash

# Xudis Auth Test Client - Quick Test Script
# This script validates that the test client can be built

set -e

BLUE='\033[0;34m'
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}测试客户端构建验证${NC}"
echo -e "${BLUE}========================================${NC}"

# Navigate to test-client directory
cd test-client

# Test 1: Maven Compile
echo -e "\n${YELLOW}[1] 测试 Maven 编译...${NC}"
mvn clean compile -q
echo -e "${GREEN}✓ 编译成功${NC}"

# Test 2: Maven Package
echo -e "\n${YELLOW}[2] 测试 Maven 打包...${NC}"
mvn package -DskipTests -q
echo -e "${GREEN}✓ 打包成功${NC}"

# Check if JAR was created
if [ -f "target/xudis-auth-test-client-1.0.0-SNAPSHOT.jar" ]; then
    echo -e "${GREEN}✓ JAR 文件创建成功${NC}"
    ls -lh target/*.jar
else
    echo -e "${RED}✗ JAR 文件未找到${NC}"
    exit 1
fi

echo -e "\n${BLUE}========================================${NC}"
echo -e "${GREEN}测试客户端构建验证完成！${NC}"
echo -e "${BLUE}========================================${NC}"

echo -e "\n${YELLOW}使用说明:${NC}"
echo -e "1. 确保认证中心正在运行: http://localhost:9000"
echo -e "2. 启动测试客户端:"
echo -e "   ${BLUE}./start-client.sh${NC}"
echo -e "   或"
echo -e "   ${BLUE}mvn spring-boot:run${NC}"
echo -e "3. 访问客户端: http://localhost:8080"
echo -e "\n详细说明请查看: ${BLUE}test-client/README.md${NC}"
