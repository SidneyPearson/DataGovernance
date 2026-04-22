package com.dataGovernance.utils;

import dm.jdbc.filter.stat.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class TokenUtil {
    private static final long TOKEN_REFRESH_THRESHOLD = 5 * 60 * 1000; // 5分钟提前刷新
    
    @Value("${ai.url}")
    private String aiUrl;
    
    @Value("${ai.username}")
    private String username;
    
    @Value("${ai.password}")
    private String password;
    
    private String accessToken;
    private long tokenExpireTime;
    private final ReentrantLock lock = new ReentrantLock();
    
    /**
     * 获取有效的access token
     */
    public String getAccessToken() {
        if (accessToken == null || isTokenExpired()) {
            refreshToken();
        }
        return accessToken;
    }
    
    /**
     * 刷新token
     */
    private void refreshToken() {
        lock.lock();
        try {
            // 双重检查锁定
            if (accessToken != null && !isTokenExpired()) {
                return;
            }
            
            RestTemplate rt = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("username", "ai:" + username);
            body.add("password", password);
            
            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = rt.postForEntity(aiUrl + "/api/user/login", entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JSONObject responseJson = new JSONObject(Objects.requireNonNull(response.getBody()));
                this.accessToken = extractAccessToken(responseJson);
                this.tokenExpireTime = System.currentTimeMillis() + 24 * 60 * 60 * 1000; // 24小时有效期
            } else {
                throw new RuntimeException("Token获取失败，状态码: " + response.getStatusCode());
            }
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * 从响应中提取access token
     */
    private String extractAccessToken(JSONObject responseJson) {
        if (responseJson.has("data")) {
            JSONObject data = responseJson.getJSONObject("data");
            if (data.has("accessToken")) {
                return data.getJSONObject("accessToken").getString("access_token");
            }
        }
        return responseJson.getString("access_token");
    }
    
    /**
     * 检查token是否即将过期
     */
    private boolean isTokenExpired() {
        return System.currentTimeMillis() > (tokenExpireTime - TOKEN_REFRESH_THRESHOLD);
    }
}
