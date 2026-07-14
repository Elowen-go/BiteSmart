package com.ws.bitesmart.service.ai;

import com.ws.bitesmart.entity.ai.AiRecommendRule;
import com.ws.bitesmart.entity.ai.NutritionStandard;
import com.ws.bitesmart.dto.request.AiRecommendRequest;
import com.ws.bitesmart.entity.dish.Dish;
import com.ws.bitesmart.mapper.dish.DishMapper;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private final DishMapper dishMapper;
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
    public Map<String, Object> recommend(Long userId, AiRecommendRequest request) {
        Map<String, Object> result = new HashMap<>();
        String mealType = request == null || request.getMealType() == null || request.getMealType().isBlank()
                ? "all" : request.getMealType().trim().toLowerCase();
        if (!Set.of("all", "breakfast", "lunch", "dinner").contains(mealType)) {
            result.put("success", false);
            result.put("message", "餐次参数不正确，请选择早餐、午餐、晚餐或全部");
            return result;
        }

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
        List<Dish> candidates = filterCandidates(dishMapper.findAvailable(), request == null ? null : request.getDietaryRestrictions());
        Map<String, Object> recipe = buildRecipe(candidates, mealType);

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

        result.putAll(recipe);

        log.info("AI推荐成功: userId={}, goal={}", userId, profile.getHealthGoal());
        return result;
    }

    private List<Dish> filterCandidates(List<Dish> dishes, String restrictions) {
        if (dishes == null) return Collections.emptyList();
        String text = restrictions == null ? "" : restrictions.toLowerCase();
        return dishes.stream()
                .filter(d -> d.getId() != null && d.getStock() != null && d.getStock() > 0)
                .filter(d -> d.getCalories() != null && d.getProtein() != null && d.getFat() != null && d.getCarbs() != null)
                .filter(d -> !matchesRestriction(d, text))
                .sorted(Comparator.comparing(Dish::getCalories).thenComparing(Dish::getId))
                .toList();
    }

    private boolean matchesRestriction(Dish dish, String restrictions) {
        if (restrictions.isBlank()) return false;
        String searchable = ((dish.getDishName() == null ? "" : dish.getDishName()) + " "
                + (dish.getSuitableFor() == null ? "" : dish.getSuitableFor())).toLowerCase();
        for (String token : restrictions.split("[,，、;；\\s]+")) {
            if (token.length() > 1 && searchable.contains(token)) return true;
            if ((token.contains("海鲜") || token.contains("海產") || token.contains("鱼") || token.contains("魚"))
                    && (searchable.contains("海鲜") || searchable.contains("虾") || searchable.contains("蟹") || searchable.contains("鱼"))) return true;
        }
        return false;
    }

    private Map<String, Object> buildRecipe(List<Dish> candidates, String requestedMeal) {
        Map<String, Object> recipe = new LinkedHashMap<>();
        List<String> mealTypes = "all".equals(requestedMeal)
                ? List.of("breakfast", "lunch", "dinner") : List.of(requestedMeal);
        List<Map<String, Object>> meals = new ArrayList<>();
        Map<String, Object> summary = nutritionMap();
        Set<Long> used = new HashSet<>();
        for (int mealIndex = 0; mealIndex < mealTypes.size(); mealIndex++) {
            String type = mealTypes.get(mealIndex);
            List<Map<String, Object>> items = new ArrayList<>();
            for (int offset = 0; offset < candidates.size() && items.size() < 2; offset++) {
                Dish dish = candidates.get((mealIndex * 2 + offset) % candidates.size());
                if (!used.add(dish.getId())) continue;
                items.add(dishMap(dish, type));
                addNutrition(summary, dish);
            }
            Map<String, Object> meal = new LinkedHashMap<>();
            meal.put("mealType", type);
            meal.put("items", items);
            meals.add(meal);
        }
        recipe.put("meals", meals);
        recipe.put("candidates", candidates.stream().map(dish -> dishMap(dish, requestedMeal)).toList());
        recipe.put("summary", summary);
        recipe.put("candidateCount", candidates.size());
        recipe.put("advice", candidates.isEmpty()
                ? "当前没有同时满足在售、库存和营养数据要求的菜品。"
                : "已从平台在售菜品中为你搭配，替换菜品后营养数据会重新计算。");
        return recipe;
    }

    private Map<String, Object> dishMap(Dish dish, String mealType) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("dishId", dish.getId());
        item.put("dishName", dish.getDishName());
        item.put("merchantId", dish.getMerchantId());
        item.put("dishImage", dish.getDishImage());
        item.put("mealType", mealType);
        item.put("quantity", 1);
        item.put("price", dish.getPrice());
        item.put("calories", dish.getCalories());
        item.put("protein", dish.getProtein());
        item.put("fat", dish.getFat());
        item.put("carbs", dish.getCarbs());
        item.put("stock", dish.getStock());
        item.put("available", true);
        return item;
    }

    private Map<String, Object> nutritionMap() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("calories", 0);
        summary.put("protein", java.math.BigDecimal.ZERO);
        summary.put("fat", java.math.BigDecimal.ZERO);
        summary.put("carbs", java.math.BigDecimal.ZERO);
        return summary;
    }

    private void addNutrition(Map<String, Object> summary, Dish dish) {
        summary.put("calories", ((Integer) summary.get("calories")) + dish.getCalories());
        summary.put("protein", ((java.math.BigDecimal) summary.get("protein")).add(dish.getProtein()));
        summary.put("fat", ((java.math.BigDecimal) summary.get("fat")).add(dish.getFat()));
        summary.put("carbs", ((java.math.BigDecimal) summary.get("carbs")).add(dish.getCarbs()));
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
