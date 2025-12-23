#requires -Version 5.1
<#
Xudis Auth Center Test Script (Windows / PowerShell)
Equivalent to test-auth-center.sh

Usage:
  powershell -ExecutionPolicy Bypass -File .\test-auth-center.ps1
  powershell -ExecutionPolicy Bypass -File .\test-auth-center.ps1 -BaseUrl "http://localhost:9000"
#>

[CmdletBinding()]
param(
  [Parameter(Mandatory = $false)]
  [string]$BaseUrl = "http://localhost:9000"
)

$ErrorActionPreference = "Stop"

function Write-Section([string]$text) {
  Write-Host ""
  Write-Host $text -ForegroundColor Yellow
}

function Write-Ok([string]$text) {
  Write-Host ("✓ " + $text) -ForegroundColor Green
}

function Write-Fail([string]$text) {
  Write-Host ("✗ " + $text) -ForegroundColor Red
}

Write-Host "========================================" -ForegroundColor Blue
Write-Host "Testing Xudis Auth Center (Windows)" -ForegroundColor Blue
Write-Host "========================================" -ForegroundColor Blue

try {
  # Test 1: OIDC Discovery
  Write-Section "[1] Testing OIDC Discovery Endpoint"
  $discovery = Invoke-RestMethod -Method Get -Uri "$BaseUrl/.well-known/openid-configuration"
  # Print first ~10 lines-ish by selecting a few common fields
  $discovery | Select-Object issuer, authorization_endpoint, token_endpoint, jwks_uri, userinfo_endpoint | Format-List
  Write-Ok "OIDC Discovery working"

  # Test 2: JWKS Endpoint
  Write-Section "[2] Testing JWKS Endpoint"
  $jwks = Invoke-RestMethod -Method Get -Uri "$BaseUrl/oauth2/jwks"
  # show first key summary
  if ($null -eq $jwks.keys -or $jwks.keys.Count -eq 0) { throw "JWKS keys is empty" }
  $jwks.keys | Select-Object -First 1 kty, kid, use, alg | Format-List
  Write-Ok "JWKS endpoint working"

  # Test 3: Custom Login API
  Write-Section "[3] Testing Custom Login API"
  $loginBody = @{
    username = "admin"
    password = "admin123!"
  } | ConvertTo-Json

  $loginResponse = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/auth/login" -ContentType "application/json" -Body $loginBody

  $accessToken = $loginResponse.accessToken
  $refreshToken = $loginResponse.refreshToken

  if ([string]::IsNullOrWhiteSpace($accessToken)) {
    Write-Fail "Login failed (no accessToken in response)"
    exit 1
  }

  $previewLen = [Math]::Min(50, $accessToken.Length)
  Write-Host ("Access Token (first {0} chars): {1}..." -f $previewLen, $accessToken.Substring(0, $previewLen))
  Write-Ok "Login successful"

  # Test 4: Refresh Token
  Write-Section "[4] Testing Refresh Token API"
  if ([string]::IsNullOrWhiteSpace($refreshToken)) {
    throw "No refreshToken returned from login"
  }

  $refreshBody = @{
    refreshToken = $refreshToken
  } | ConvertTo-Json

  $refreshResponse = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/auth/refresh" -ContentType "application/json" -Body $refreshBody
  $newAccessToken = $refreshResponse.accessToken

  if ([string]::IsNullOrWhiteSpace($newAccessToken)) {
    Write-Fail "Token refresh failed (no accessToken in response)"
    exit 1
  }

  $previewLen2 = [Math]::Min(50, $newAccessToken.Length)
  Write-Host ("New Access Token (first {0} chars): {1}..." -f $previewLen2, $newAccessToken.Substring(0, $previewLen2))
  Write-Ok "Token refresh successful"

  # Test 5: OAuth2 Client Credentials Flow
  Write-Section "[5] Testing OAuth2 Client Credentials Flow"
  $basic = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("demo-client:demo-secret"))

  $oauth2Headers = @{
    Authorization = "Basic $basic"
    "Content-Type" = "application/x-www-form-urlencoded"
  }

  $oauth2Body = "grant_type=client_credentials&scope=read%20write"

  $oauth2Response = Invoke-RestMethod -Method Post -Uri "$BaseUrl/oauth2/token" -Headers $oauth2Headers -Body $oauth2Body
  $oauth2Token = $oauth2Response.access_token

  if ([string]::IsNullOrWhiteSpace($oauth2Token)) {
    Write-Fail "OAuth2 client credentials failed (no access_token in response)"
    exit 1
  }

  $previewLen3 = [Math]::Min(50, $oauth2Token.Length)
  Write-Host ("OAuth2 Access Token (first {0} chars): {1}..." -f $previewLen3, $oauth2Token.Substring(0, $previewLen3))
  Write-Ok "OAuth2 client credentials working"

  # Test 6: Logout
  Write-Section "[6] Testing Logout API"
  $logoutHeaders = @{
    Authorization = "Bearer $accessToken"
  }

  # Some APIs return plain text; use Invoke-WebRequest to reliably read raw content
  $logoutResp = Invoke-WebRequest -Method Post -Uri "$BaseUrl/api/auth/logout" -Headers $logoutHeaders
  $logoutText = $logoutResp.Content

  if ($logoutText -match "Logged out successfully") {
    Write-Ok "Logout successful"
  } else {
    Write-Fail ("Logout failed. Response: " + $logoutText)
    exit 1
  }

  Write-Host ""
  Write-Host "========================================" -ForegroundColor Blue
  Write-Host "All tests passed successfully!" -ForegroundColor Green
  Write-Host "========================================" -ForegroundColor Blue
}
catch {
  Write-Host ""
  Write-Fail ("Test script error: " + $_.Exception.Message)
  exit 1
}