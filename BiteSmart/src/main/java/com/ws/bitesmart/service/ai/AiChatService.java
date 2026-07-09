package com.ws.bitesmart.service.ai;

import com.ws.bitesmart.common.util.SnowflakeUtil;
import com.ws.bitesmart.entity.ai.AiConversation;
import com.ws.bitesmart.mapper.ai.AiConversationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * AI对话服务
 *
 * 接入 DeepSeek API，支持多轮对话上下文。
 * API 地址和 Key 在 application.yml 中配置。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final AiConversationMapper aiConversationMapper;
    private final RestTemplate restTemplate;

    @Value("${ai.api-key}")
    private String apiKey;

    @Value("${ai.api-url}")
    private String apiUrl;

    @Value("${ai.model}")
    private String modelName;

    /**
     * AI 对话
     *
     * @param userId    当前用户ID
     * @param sessionId 会话ID（首次传空，后端自动生成）
     * @param question  用户提问
     * @return 本次问答记录（含AI回复）
     */
    @Transactional
    public AiConversation chat(Long userId, String sessionId, String question) {
        // 首次对话自动生成 sessionId
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString().replace("-", "");
        }

        // 记录开始时间
        long startTime = System.currentTimeMillis();

        // 调用 DeepSeek API
        String answer = callDeepSeekApi(question, userId, sessionId);

        // 计算耗时
        long responseTime = System.currentTimeMillis() - startTime;

        // 保存到数据库
        AiConversation conversation = new AiConversation();
        conversation.setId(SnowflakeUtil.generate());
        conversation.setUserId(userId);
        conversation.setSessionId(sessionId);
        conversation.setQuestion(question);
        conversation.setAnswer(answer);
        conversation.setModelName(modelName);
        conversation.setPromptTokens(question.length());
        conversation.setCompletionTokens(answer.length());
        conversation.setTotalTokens(question.length() + answer.length());
        conversation.setCost(BigDecimal.ZERO);
        conversation.setResponseTime((int) responseTime);

        aiConversationMapper.insert(conversation);

        log.info("AI对话: userId={}, sessionId={}, 耗时={}ms", userId, sessionId, responseTime);
        return conversation;
    }

    /**
     * 调用 DeepSeek API
     *
     * 使用 OpenAI 兼容接口格式。
     */
    private String callDeepSeekApi(String question, Long userId, String sessionId) {
        try {
            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            // 构建消息体
            List<Map<String, String>> messages = new ArrayList<>();

            // System prompt
            Map<String, String> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", "你是BiteSmart智能健康膳食平台的AI助手，专门为用户提供健康饮食建议、营养指导和食谱推荐。"
                    + "请用中文回答，语气亲切专业。回答要简洁实用，可以给出具体的食物建议和营养数据。"
                    + "如果用户问的问题与健康饮食无关，可以礼貌地引导回健康话题。");
            messages.add(systemMsg);

            // User question
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", question);
            messages.add(userMsg);

            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", modelName);
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 1024);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // 发送请求
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);

            // 解析响应
            if (response.getBody() != null && response.getBody().containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    Map<String, String> message = (Map<String, String>) choice.get("message");
                    return message.getOrDefault("content", "抱歉，我现在无法回答，请稍后再试。");
                }
            }

            // 解析失败时的处理
            if (response.getBody() != null && response.getBody().containsKey("error")) {
                Map<String, Object> error = (Map<String, Object>) response.getBody().get("error");
                log.error("DeepSeek API 返回错误: {}", error.get("message"));
            }

            return "抱歉，AI 服务暂时不可用，请稍后再试。";

        } catch (Exception e) {
            log.error("调用 DeepSeek API 失败: {}", e.getMessage());
            return "抱歉，AI 服务连接失败，请检查网络后重试。";
        }
    }

    /** 查某个会话的历史记录 */
    public List<AiConversation> getHistory(String sessionId) {
        return aiConversationMapper.findBySessionId(sessionId);
    }

    /** 查用户的所有历史消息 */
    public List<AiConversation> getUserMessages(Long userId) {
        return aiConversationMapper.findSessionsByUserId(userId);
    }

}
