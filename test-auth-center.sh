#!/bin/bash

# Xudis Auth Center Test Script
# This script demonstrates all major authentication and authorization features

set -e

BASE_URL="http://localhost:9000"
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Testing Xudis Auth Center${NC}"
echo -e "${BLUE}========================================${NC}"

# Test 1: OIDC Discovery
echo -e "\n${YELLOW}[1] Testing OIDC Discovery Endpoint${NC}"
curl -s "${BASE_URL}/.well-known/openid-configuration" | python3 -m json.tool | head -10
echo -e "${GREEN}✓ OIDC Discovery working${NC}"

# Test 2: JWKS Endpoint
echo -e "\n${YELLOW}[2] Testing JWKS Endpoint${NC}"
curl -s "${BASE_URL}/oauth2/jwks" | python3 -m json.tool | head -5
echo -e "${GREEN}✓ JWKS endpoint working${NC}"

# Test 3: Custom Login API
echo -e "\n${YELLOW}[3] Testing Custom Login API${NC}"
LOGIN_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123!"}')

ACCESS_TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
REFRESH_TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"refreshToken":"[^"]*"' | cut -d'"' -f4)

if [ -n "$ACCESS_TOKEN" ]; then
  echo "Access Token (first 50 chars): ${ACCESS_TOKEN:0:50}..."
  echo -e "${GREEN}✓ Login successful${NC}"
else
  echo -e "${RED}✗ Login failed${NC}"
  exit 1
fi

# Test 4: Refresh Token
echo -e "\n${YELLOW}[4] Testing Refresh Token API${NC}"
REFRESH_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/auth/refresh" \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"$REFRESH_TOKEN\"}")

NEW_ACCESS_TOKEN=$(echo $REFRESH_RESPONSE | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)

if [ -n "$NEW_ACCESS_TOKEN" ]; then
  echo "New Access Token (first 50 chars): ${NEW_ACCESS_TOKEN:0:50}..."
  echo -e "${GREEN}✓ Token refresh successful${NC}"
else
  echo -e "${RED}✗ Token refresh failed${NC}"
  exit 1
fi

# Test 5: OAuth2 Client Credentials Flow
echo -e "\n${YELLOW}[5] Testing OAuth2 Client Credentials Flow${NC}"
OAUTH2_RESPONSE=$(curl -s -X POST "${BASE_URL}/oauth2/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -u demo-client:demo-secret \
  -d "grant_type=client_credentials&scope=read write")

OAUTH2_TOKEN=$(echo $OAUTH2_RESPONSE | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)

if [ -n "$OAUTH2_TOKEN" ]; then
  echo "OAuth2 Access Token (first 50 chars): ${OAUTH2_TOKEN:0:50}..."
  echo -e "${GREEN}✓ OAuth2 client credentials working${NC}"
else
  echo -e "${RED}✗ OAuth2 client credentials failed${NC}"
  exit 1
fi

# Test 6: Logout
echo -e "\n${YELLOW}[6] Testing Logout API${NC}"
LOGOUT_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/auth/logout" \
  -H "Authorization: Bearer $ACCESS_TOKEN")

if echo "$LOGOUT_RESPONSE" | grep -q "Logged out successfully"; then
  echo -e "${GREEN}✓ Logout successful${NC}"
else
  echo -e "${RED}✗ Logout failed${NC}"
  exit 1
fi

echo -e "\n${BLUE}========================================${NC}"
echo -e "${GREEN}All tests passed successfully!${NC}"
echo -e "${BLUE}========================================${NC}"
