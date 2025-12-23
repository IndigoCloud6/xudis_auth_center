package com.xudis.authclient.controller;

import com.xudis.authclient.dto.LoginResponse;
import com.xudis.authclient.service.AuthCenterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class TestController {

    private final AuthCenterService authCenterService;

    @GetMapping("/")
    public String home(Model model, @AuthenticationPrincipal OidcUser principal) {
        if (principal != null) {
            model.addAttribute("username", principal.getPreferredUsername());
            model.addAttribute("authenticated", true);
        } else {
            model.addAttribute("authenticated", false);
        }
        return "index";
    }

    @GetMapping("/oauth2/success")
    public String oauth2Success(@AuthenticationPrincipal OidcUser principal, Model model) {
        log.info("OAuth2 login successful for user: {}", principal.getPreferredUsername());
        
        model.addAttribute("username", principal.getPreferredUsername());
        model.addAttribute("email", principal.getEmail());
        model.addAttribute("subject", principal.getSubject());
        model.addAttribute("claims", principal.getClaims());
        model.addAttribute("idToken", principal.getIdToken().getTokenValue());
        
        return "oauth2-success";
    }

    @GetMapping("/test/custom-api")
    public String testCustomApiPage() {
        return "test-custom-api";
    }

    @PostMapping("/test/login")
    public String testLogin(@RequestParam String username, 
                          @RequestParam String password,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        try {
            LoginResponse response = authCenterService.login(username, password);
            
            model.addAttribute("success", true);
            model.addAttribute("accessToken", response.getAccessToken());
            model.addAttribute("refreshToken", response.getRefreshToken());
            model.addAttribute("tokenType", response.getTokenType());
            model.addAttribute("expiresIn", response.getExpiresIn());
            
            // Parse access token to show claims
            if (response.getAccessToken() != null) {
                try {
                    Map<String, Object> claims = authCenterService.parseJwt(response.getAccessToken());
                    model.addAttribute("claims", claims);
                } catch (Exception e) {
                    log.warn("Failed to parse JWT claims: {}", e.getMessage());
                }
            }
            
            return "test-custom-api";
        } catch (Exception e) {
            log.error("Login test failed", e);
            model.addAttribute("error", "Login failed: " + e.getMessage());
            return "test-custom-api";
        }
    }

    @PostMapping("/test/refresh")
    public String testRefresh(@RequestParam String refreshToken, 
                            Model model) {
        try {
            LoginResponse response = authCenterService.refresh(refreshToken);
            
            model.addAttribute("success", true);
            model.addAttribute("refreshSuccess", true);
            model.addAttribute("accessToken", response.getAccessToken());
            model.addAttribute("refreshToken", response.getRefreshToken());
            model.addAttribute("tokenType", response.getTokenType());
            model.addAttribute("expiresIn", response.getExpiresIn());
            
            // Parse access token to show claims
            if (response.getAccessToken() != null) {
                try {
                    Map<String, Object> claims = authCenterService.parseJwt(response.getAccessToken());
                    model.addAttribute("claims", claims);
                } catch (Exception e) {
                    log.warn("Failed to parse JWT claims: {}", e.getMessage());
                }
            }
            
            return "test-custom-api";
        } catch (Exception e) {
            log.error("Refresh test failed", e);
            model.addAttribute("error", "Token refresh failed: " + e.getMessage());
            return "test-custom-api";
        }
    }

    @PostMapping("/test/logout")
    public String testLogout(@RequestParam String accessToken, 
                           Model model) {
        try {
            String response = authCenterService.logout(accessToken);
            
            model.addAttribute("success", true);
            model.addAttribute("logoutSuccess", true);
            model.addAttribute("logoutMessage", response);
            
            return "test-custom-api";
        } catch (Exception e) {
            log.error("Logout test failed", e);
            model.addAttribute("error", "Logout failed: " + e.getMessage());
            return "test-custom-api";
        }
    }

    @GetMapping("/test/discovery")
    public String testDiscovery(Model model) {
        try {
            Map<String, Object> config = authCenterService.getOidcConfiguration();
            model.addAttribute("success", true);
            model.addAttribute("config", config);
            return "test-discovery";
        } catch (Exception e) {
            log.error("Discovery test failed", e);
            model.addAttribute("error", "OIDC Discovery failed: " + e.getMessage());
            return "test-discovery";
        }
    }

    @GetMapping("/test/jwks")
    public String testJwks(Model model) {
        try {
            Map<String, Object> jwks = authCenterService.getJwks();
            model.addAttribute("success", true);
            model.addAttribute("jwks", jwks);
            return "test-jwks";
        } catch (Exception e) {
            log.error("JWKS test failed", e);
            model.addAttribute("error", "JWKS retrieval failed: " + e.getMessage());
            return "test-jwks";
        }
    }

    @GetMapping("/test/client-credentials")
    public String testClientCredentialsPage() {
        return "test-client-credentials";
    }

    @PostMapping("/test/client-credentials")
    public String testClientCredentials(@RequestParam String clientId,
                                      @RequestParam String clientSecret,
                                      @RequestParam String scope,
                                      Model model) {
        try {
            Map<String, Object> response = authCenterService.getClientCredentialsToken(clientId, clientSecret, scope);
            
            model.addAttribute("success", true);
            model.addAttribute("tokenResponse", response);
            
            // Parse access token if present
            if (response.containsKey("access_token")) {
                try {
                    String accessToken = (String) response.get("access_token");
                    Map<String, Object> claims = authCenterService.parseJwt(accessToken);
                    model.addAttribute("claims", claims);
                } catch (Exception e) {
                    log.warn("Failed to parse JWT claims: {}", e.getMessage());
                }
            }
            
            return "test-client-credentials";
        } catch (Exception e) {
            log.error("Client Credentials test failed", e);
            model.addAttribute("error", "Client Credentials flow failed: " + e.getMessage());
            return "test-client-credentials";
        }
    }
}
