package com.dataGovernance.utils;

import dm.jdbc.filter.stat.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;

@Component
public class AISessionUtil {
    
    @Autowired
    private RestTemplate restTemplate;

    @Value("${ai.url}")
    private String baseUrl;
    
    @Value("${ai.application_id}")
    private String aiApplicationId;
    
    /**
     * 创建新会话
     * @param headers Authorization请求头
     * @return 会话ID
     */
    public String createSession(HttpHeaders headers) {
        try {
            String url = baseUrl + "/api/ai/create_session";
            
            JSONObject requestBody = new JSONObject();
            requestBody.put("application_id", aiApplicationId);
            
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, 
                HttpMethod.POST, 
                entity, 
                String.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JSONObject responseJson = new JSONObject(Objects.requireNonNull(response.getBody()));
                return responseJson.getJSONObject("data").getString("data");
            }
            
            throw new RuntimeException("创建会话失败: " + response.getStatusCode());
            
        } catch (Exception e) {
            throw new RuntimeException("创建会话异常", e);
        }
    }
    
    /**
     * 停止会话
     * @param conversationId 会话ID
     * @param headers Authorization请求头
     * @return 是否成功
     */
    public boolean stopSession(HttpHeaders headers, String conversationId) {
        try {
            String url = baseUrl + "/api/ai/stop";
            
            JSONObject requestBody = new JSONObject();
            requestBody.put("conversation_id", conversationId);
            requestBody.put("application_id", aiApplicationId);
            
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, 
                HttpMethod.POST, 
                entity, 
                String.class
            );
            
            return response.getStatusCode() == HttpStatus.OK;
            
        } catch (Exception e) {
            throw new RuntimeException("停止会话异常", e);
        }
    }
}
