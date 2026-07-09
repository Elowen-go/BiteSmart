package com.ws.bitesmart.service.ai;

import com.ws.bitesmart.entity.ai.AiRecommendRule;
import com.ws.bitesmart.entity.ai.NutritionStandard;
import com.ws.bitesmart.entity.user.UserProfile;
import com.ws.bitesmart.mapper.ai.AiRecommendRuleMapper;
import com.ws.bitesmart.mapper.ai.NutritionStandardMapper;
import com.ws.bitesmart.mapper.user.UserProfileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
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

        // 4. 组装结果
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

        // 生成推荐说明
        result.put("recommendation", buildRecommendation(profile, standard, rule));

        log.info("AI推荐成功: userId={}, goal={}", userId, profile.getHealthGoal());
        return result;
    }

    /**
     * 根据用户档案和标准生成推荐说明
     */
    private String buildRecommendation(UserProfile profile, NutritionStandard standard, AiRecommendRule rule) {
        StringBuilder sb = new StringBuilder();

        // 目标
        String goalName = switch (profile.getHealthGoal()) {
            case "减肥" -> "减脂";
            case "增肌" -> "增肌";
            case "控糖" -> "控糖";
            default -> "维持健康";
        };
        sb.append("### ").append(goalName).append("饮食建议\n\n");

        // 热量
        if (standard != null) {
            sb.append("根据你的个人情况，建议每日摄入 **").append(standard.getDailyCalories())
                    .append("大卡** 热量。\n\n");
            sb.append("- 蛋白质：").append(standard.getProteinGrams()).append("g\n");
            sb.append("- 脂肪：").append(standard.getFatGrams()).append("g\n");
            sb.append("- 碳水：").append(standard.getCarbsGrams()).append("g\n");
            sb.append("- 饮水：").append(standard.getWaterMl() != null ? standard.getWaterMl() + "ml" : "充足饮水").append("\n\n");
        }

        // 根据目标给建议
        sb.append("### 饮食原则\n\n");
        switch (profile.getHealthGoal()) {
            case "减肥" -> sb.append("""
                    1. 制造热量缺口，每天减少 300-500 大卡摄入
                    2. 增加膳食纤维，提升饱腹感
                    3. 选择优质蛋白，避免肌肉流失
                    4. 少吃精制碳水和油炸食品
                    5. 细嚼慢咽，每餐七分饱
                    """);
            case "增肌" -> sb.append("""
                    1. 保证热量盈余，每天多摄入 300-500 大卡
                    2. 足量蛋白质，每公斤体重 1.6-2.0g
                    3. 训练后及时补充碳水和蛋白质
                    4. 少食多餐，每天 4-6 餐
                    """);
            case "控糖" -> sb.append("""
                    1. 选择低GI食物，稳定血糖
                    2. 控制碳水总量，每餐不超过 50g
                    3. 增加蔬菜摄入，每餐至少 200g
                    4. 避免含糖饮料和甜点
                    5. 饭后散步 15 分钟
                    """);
            default -> sb.append("""
                    1. 均衡饮食，保证各类营养素摄入
                    2. 三餐规律，不暴饮暴食
                    3. 多吃蔬菜水果，适量优质蛋白
                    4. 少油少盐少糖
                    """);
        }

        sb.append("\n你可以根据这些建议，在平台上选择合适的健康套餐！");
        return sb.toString();
    }

}
