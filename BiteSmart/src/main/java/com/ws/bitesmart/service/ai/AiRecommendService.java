package com.ws.bitesmart.service.ai;

import com.ws.bitesmart.entity.ai.AiRecommendRule;
import com.ws.bitesmart.entity.ai.NutritionStandard;
import com.ws.bitesmart.entity.user.UserProfile;
import com.ws.bitesmart.mapper.ai.AiRecommendRuleMapper;
import com.ws.bitesmart.mapper.ai.NutritionStandardMapper;
import com.ws.bitesmart.mapper.user.UserProfileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI食谱推荐服务
 *
 * 根据用户的健康档案和营养标准，生成个性化的饮食推荐。
 * 如果用户没有填健康档案，返回推荐让用户先去填写。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiRecommendService {

    private final UserProfileMapper userProfileMapper;
    private final NutritionStandardMapper nutritionStandardMapper;
    private final AiRecommendRuleMapper aiRecommendRuleMapper;
    private final RestTemplate restTemplate;

    @Value("${ai.api-key}")
    private String apiKey;

    @Value("${ai.api-url}")
    private String apiUrl;

    @Value("${ai.model}")
    private String modelName;

    /**
     * 为用户生成食谱推荐
     *
     * @param userId 用户ID
     * @return 包含推荐结果的 Map，含营养目标和推荐说明
     */
    public Map<String, Object> recommend(Long userId) {
        Map<String, Object> result = new HashMap<>();

        // 1. 获取用户健康档案
        UserProfile profile = userProfileMapper.findByUserId(userId);
        if (profile == null || profile.getHealthGoal() == null) {
            result.put("success", false);
            result.put("message", "请先填写健康档案，才能为你推荐合适的饮食方案");
            return result;
        }

        // 2. 匹配营养标准
        NutritionStandard standard = nutritionStandardMapper.findBestMatch(
                profile.getGender(),
                profile.getAge(),
                profile.getActivityLevel(),
                profile.getHealthGoal()
        );

        // 3. 匹配推荐规则
        AiRecommendRule rule = aiRecommendRuleMapper.findBestRuleByGoal(profile.getHealthGoal());

        // 4. 调用 DeepSeek 生成个性化食谱
        String recommendation = callDeepSeekForRecommendation(profile, standard, rule);

        // 5. 组装结果
        result.put("success", true);
        result.put("healthGoal", profile.getHealthGoal());
        result.put("userProfile", profile);

        if (standard != null) {
            result.put("nutritionStandard", standard);
            result.put("dailyCalories", standard.getDailyCalories());
            result.put("proteinGrams", standard.getProteinGrams());
            result.put("fatGrams", standard.getFatGrams());
            result.put("carbsGrams", standard.getCarbsGrams());
        }

        if (rule != null) {
            result.put("recommendRule", rule);
        }

        result.put("recommendation", recommendation);

        log.info("AI推荐成功: userId={}, goal={}", userId, profile.getHealthGoal());
        return result;
    }

    /**
     * 调用 DeepSeek API 生成个性化饮食推荐
     */
    private String callDeepSeekForRecommendation(UserProfile profile, NutritionStandard standard, AiRecommendRule rule) {
        try {
            StringBuilder prompt = new StringBuilder();
            prompt.append("请根据以下用户信息，生成一份详细的每日饮食推荐方案。\n\n");

            // 用户信息
            String genderStr = profile.getGender() == 10 ? "男" : "女";
            prompt.append("用户信息：\n");
            prompt.append("- 年龄：").append(profile.getAge()).append("岁\n");
            prompt.append("- 性别：").append(genderStr).append("\n");
            prompt.append("- 身高：").append(profile.getHeight()).append("cm\n");
            prompt.append("- 体重：").append(profile.getWeight()).append("kg\n");
            prompt.append("- 目标：").append(profile.getHealthGoal()).append("\n");
            String activityStr = switch (profile.getActivityLevel() != null ? profile.getActivityLevel() : 20) {
                case 10 -> "久坐";
                case 20 -> "轻度运动";
                case 30 -> "中度运动";
                case 40 -> "重度运动";
                default -> "轻度运动";
            };
            prompt.append("- 运动量：").append(activityStr).append("\n");

            if (profile.getAllergyInfo() != null && !profile.getAllergyInfo().isEmpty()) {
                prompt.append("- 过敏信息：").append(profile.getAllergyInfo()).append("\n");
            }
            if (profile.getDiseaseHistory() != null && !profile.getDiseaseHistory().isEmpty()) {
                prompt.append("- 疾病史：").append(profile.getDiseaseHistory()).append("\n");
            }

            // 营养标准
            if (standard != null) {
                prompt.append("\n推荐营养标准：\n");
                prompt.append("- 每日热量：").append(standard.getDailyCalories()).append("大卡\n");
                prompt.append("- 蛋白质：").append(standard.getProteinGrams()).append("g\n");
                prompt.append("- 脂肪：").append(standard.getFatGrams()).append("g\n");
                prompt.append("- 碳水：").append(standard.getCarbsGrams()).append("g\n");
            }

            prompt.append("\n请提供以下内容：\n");
            prompt.append("1. **总体饮食原则**（3-5条，针对用户目标）\n");
            prompt.append("2. **一日三餐推荐食谱**（具体到食物名称和份量）\n");
            prompt.append("3. **营养素分析**（每餐的热量、蛋白质、脂肪、碳水分布）\n");
            prompt.append("4. **进食建议**（什么时间吃、怎么吃更好）\n");
            prompt.append("5. **推荐搭配**（适合该用户的平台套餐类型）\n\n");
            prompt.append("请用中文回答，格式清晰，以 Markdown 格式输出。");

            // 调用 DeepSeek
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", "你是BiteSmart智能健康膳食平台的营养专家，擅长根据用户的身体数据和健康目标，生成科学、可执行的每日饮食方案。你的回答要专业、具体、实用。");
            messages.add(systemMsg);

            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", prompt.toString());
            messages.add(userMsg);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", modelName);
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.8);
            requestBody.put("max_tokens", 2048);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);

            if (response.getBody() != null && response.getBody().containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    Map<String, String> message = (Map<String, String>) choice.get("message");
                    return message.getOrDefault("content", "无法生成推荐方案，请稍后再试。");
                }
            }

            return "无法生成推荐方案，请稍后再试。";

        } catch (Exception e) {
            log.error("调用 DeepSeek 生成推荐失败: {}", e.getMessage());
            return "个性化推荐服务暂时不可用，请稍后再试。你可以参考平台上的健康套餐进行选择。";
        }
    }

}
