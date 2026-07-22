package com.ws.bitesmart.service.plan;

import com.ws.bitesmart.common.enums.ResultCodeEnum;
import com.ws.bitesmart.common.util.SnowflakeUtil;
import com.ws.bitesmart.entity.dish.Dish;
import com.ws.bitesmart.entity.health.DietRecord;
import com.ws.bitesmart.entity.plan.UserPlan;
import com.ws.bitesmart.entity.plan.UserPlanMeal;
import com.ws.bitesmart.entity.user.UserProfile;
import com.ws.bitesmart.exception.BusinessException;
import com.ws.bitesmart.mapper.dish.DishMapper;
import com.ws.bitesmart.mapper.health.DietRecordMapper;
import com.ws.bitesmart.mapper.plan.UserPlanMapper;
import com.ws.bitesmart.mapper.plan.UserPlanMealMapper;
import com.ws.bitesmart.mapper.user.UserMembershipMapper;
import com.ws.bitesmart.mapper.user.UserProfileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 专属计划服务
 *
 * 根据用户健康档案生成 N 天 × 3 餐的专属食谱计划（默认 7 天，会员可 21 天）。
 *
 * 菜品挑选规则（对齐小程序原型 planMeals 逻辑）：
 *   1. 菜品池 = dish 表中上架且有库存的菜品，按 id 升序排列（保证偏移稳定）；
 *      再按健康目标做软过滤：减脂/减重目标只留 ≤450kcal 的菜品，
 *      增肌目标只留蛋白质 ≥20g 的菜品，过滤后不足 3 道则回退到全量池。
 *   2. 第 day 天第 i 餐的菜品下标 = (day * 3 + i + swapCount) % poolSize，
 *      即每天 3 餐依次向后滚动，换一道菜就是 swapCount+1 后取下一道。
 *
 * 打卡同步：打卡时写入一条 diet_record（source_type=30 计划餐，
 * foodName 带"（计划餐）"后缀，plan_meal_id 关联），取消打卡按 plan_meal_id 删除。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanService {

    /** 计划餐写入 diet_record 的来源枚举：30-计划餐打卡 */
    public static final int DIET_SOURCE_PLAN = 30;

    private static final int DEFAULT_PLAN_DAYS = 7;
    private static final int MEMBER_PLAN_DAYS = 21;
    private static final int MEALS_PER_DAY = 3;

    private final UserPlanMapper userPlanMapper;
    private final UserPlanMealMapper userPlanMealMapper;
    private final UserProfileMapper userProfileMapper;
    private final DishMapper dishMapper;
    private final DietRecordMapper dietRecordMapper;
    private final UserMembershipMapper userMembershipMapper;

    /**
     * 生成专属计划：基于当前用户健康档案，写入 user_plan + planDays×3 条计划餐。
     * 旧的未开始/进行中计划会被置为已取消。
     *
     * @param requestedDays 期望天数：7 或 21，其他值按 7 处理；
     *                      请求 21 天但无生效会员时自动降级为 7 天
     */
    @Transactional
    public Map<String, Object> generate(Long userId, Integer requestedDays) {
        UserProfile profile = userProfileMapper.findByUserId(userId);
        if (profile == null) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "请先完善健康档案再生成计划");
        }
        int planDays = resolvePlanDays(userId, requestedDays);
        List<Dish> pool = buildDishPool(profile.getHealthGoal());

        // 重新生成时取消旧计划
        userPlanMapper.cancelActiveByUserId(userId);

        UserPlan plan = new UserPlan();
        plan.setId(SnowflakeUtil.generate());
        plan.setUserId(userId);
        plan.setPlanDays(planDays);
        plan.setGoal(profile.getHealthGoal());
        plan.setActivityLevel(profile.getActivityLevel());
        plan.setTargetWeight(profile.getTargetWeight());
        plan.setStartWeight(profile.getWeight());
        plan.setPrefs(profile.getDietPreference());
        plan.setAvoid(profile.getAllergyInfo());
        plan.setFocusParts(profile.getFocusParts());
        plan.setStatus(UserPlan.STATUS_NOT_STARTED);
        plan.setCurDay(0);
        userPlanMapper.insert(plan);

        for (int day = 0; day < planDays; day++) {
            for (int meal = 0; meal < MEALS_PER_DAY; meal++) {
                UserPlanMeal m = new UserPlanMeal();
                m.setId(SnowflakeUtil.generate());
                m.setPlanId(plan.getId());
                m.setDayIndex(day);
                m.setMealIndex(meal);
                m.setDishId(pickDish(pool, day, meal, 0).getId());
                m.setSwapCount(0);
                m.setChecked(0);
                userPlanMealMapper.insert(m);
            }
        }
        log.info("专属计划已生成: userId={}, planId={}, poolSize={}", userId, plan.getId(), pool.size());
        return buildPlanDetail(plan);
    }

    /** 当前用户最新计划 + 全部餐次（没有计划时 data 为 null） */
    public Map<String, Object> current(Long userId) {
        UserPlan plan = userPlanMapper.findLatestByUserId(userId);
        if (plan == null) return null;
        return buildPlanDetail(plan);
    }

    /** 开始执行计划：status 10→20，cur_day=0 */
    @Transactional
    public Map<String, Object> start(Long userId) {
        UserPlan plan = requireLatestPlan(userId);
        if (!Integer.valueOf(UserPlan.STATUS_NOT_STARTED).equals(plan.getStatus())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "只有未开始的计划才能开始执行");
        }
        plan.setStatus(UserPlan.STATUS_RUNNING);
        plan.setCurDay(0);
        plan.setStartedTime(LocalDateTime.now());
        userPlanMapper.updateById(plan);
        return buildPlanDetail(plan);
    }

    /**
     * 打卡/取消打卡。
     * 只允许对进行中计划的当天（cur_day）餐次操作；
     * 打卡时同步写入 diet_record，取消时删除对应记录。
     */
    @Transactional
    public Map<String, Object> check(Long userId, Long mealId, boolean checked) {
        UserPlanMeal meal = requireOwnedMeal(userId, mealId);
        UserPlan plan = userPlanMapper.findById(meal.getPlanId());
        if (!Integer.valueOf(UserPlan.STATUS_RUNNING).equals(plan.getStatus())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "计划未在执行中，不能打卡");
        }
        if (!plan.getCurDay().equals(meal.getDayIndex())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "只能打卡当天餐食");
        }

        if (checked) {
            if (Integer.valueOf(1).equals(meal.getChecked())) {
                throw new BusinessException(ResultCodeEnum.CONFLICT, "该餐已打卡");
            }
            meal.setChecked(1);
            meal.setCheckedTime(LocalDateTime.now());
            userPlanMealMapper.updateById(meal);
            insertPlanDietRecord(userId, meal);
        } else {
            if (!Integer.valueOf(1).equals(meal.getChecked())) {
                throw new BusinessException(ResultCodeEnum.CONFLICT, "该餐尚未打卡");
            }
            meal.setChecked(0);
            meal.setClearCheckedTime(true);
            userPlanMealMapper.updateById(meal);
            DietRecord record = dietRecordMapper.findByPlanMealId(mealId);
            if (record != null) {
                dietRecordMapper.deleteById(record.getId());
            }
        }
        return buildPlanDetail(plan);
    }

    /**
     * 换一道菜：swap_count+1，按菜品池偏移 (day*3 + mealIndex + swapCount) 重新选菜。
     * 已打卡的餐次不允许更换（饮食记录已同步）。
     */
    @Transactional
    public Map<String, Object> swap(Long userId, Long mealId) {
        UserPlanMeal meal = requireOwnedMeal(userId, mealId);
        UserPlan plan = userPlanMapper.findById(meal.getPlanId());
        if (Integer.valueOf(UserPlan.STATUS_FINISHED).equals(plan.getStatus())
                || Integer.valueOf(UserPlan.STATUS_CANCELLED).equals(plan.getStatus())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "计划已结束，不能换菜");
        }
        if (Integer.valueOf(1).equals(meal.getChecked())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "已打卡的餐食不能更换");
        }

        int swapCount = (meal.getSwapCount() == null ? 0 : meal.getSwapCount()) + 1;
        List<Dish> pool = buildDishPool(plan.getGoal());
        Dish dish = pickDish(pool, meal.getDayIndex(), meal.getMealIndex(), swapCount);

        meal.setDishId(dish.getId());
        meal.setSwapCount(swapCount);
        userPlanMealMapper.updateById(meal);
        return buildPlanDetail(plan);
    }

    /** 演示用：进入下一天。最后一天自动标记计划完成。 */
    @Transactional
    public Map<String, Object> advance(Long userId) {
        UserPlan plan = requireLatestPlan(userId);
        if (!Integer.valueOf(UserPlan.STATUS_RUNNING).equals(plan.getStatus())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "计划未在执行中");
        }
        int next = plan.getCurDay() + 1;
        if (next >= plan.getPlanDays()) {
            plan.setStatus(UserPlan.STATUS_FINISHED);
        } else {
            plan.setCurDay(next);
        }
        userPlanMapper.updateById(plan);
        return buildPlanDetail(plan);
    }

    // ==================== 内部方法 ====================

    /**
     * 解析计划天数：只接受 7 或 21，其他值按 7；
     * 请求 21 天时校验生效会员（status=10 且未过期），没有则降级为 7。
     */
    private int resolvePlanDays(Long userId, Integer requestedDays) {
        if (requestedDays == null || requestedDays != MEMBER_PLAN_DAYS) {
            return DEFAULT_PLAN_DAYS;
        }
        boolean hasActiveMembership = userMembershipMapper.findActiveByUserId(userId) != null;
        if (!hasActiveMembership) {
            log.info("用户 {} 请求 21 天计划但无生效会员，降级为 7 天", userId);
            return DEFAULT_PLAN_DAYS;
        }
        return MEMBER_PLAN_DAYS;
    }

    private UserPlan requireLatestPlan(Long userId) {
        UserPlan plan = userPlanMapper.findLatestByUserId(userId);
        if (plan == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "还没有生成计划");
        }
        return plan;
    }

    /** 查餐次并校验属于当前用户 */
    private UserPlanMeal requireOwnedMeal(Long userId, Long mealId) {
        UserPlanMeal meal = userPlanMealMapper.findById(mealId);
        if (meal == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "计划餐次不存在");
        }
        UserPlan plan = userPlanMapper.findById(meal.getPlanId());
        if (plan == null || !plan.getUserId().equals(userId)) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "计划餐次不存在");
        }
        return meal;
    }

    /**
     * 构建菜品池：上架且有库存的菜品按 id 升序，再按健康目标软过滤。
     */
    private List<Dish> buildDishPool(String goal) {
        List<Dish> all = dishMapper.findPlanPool();
        if (all.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "暂无可选菜品，无法生成计划");
        }
        List<Dish> filtered = all;
        if (goal != null) {
            if (goal.contains("减") || goal.contains("瘦") || goal.contains("控糖")) {
                filtered = all.stream()
                        .filter(d -> d.getCalories() != null && d.getCalories() <= 450)
                        .collect(Collectors.toList());
            } else if (goal.contains("增肌")) {
                filtered = all.stream()
                        .filter(d -> d.getProtein() != null && d.getProtein().doubleValue() >= 20)
                        .collect(Collectors.toList());
            }
        }
        return filtered.size() >= MEALS_PER_DAY ? filtered : all;
    }

    /** 按轮换偏移选菜：下标 = (day*3 + mealIndex + swapCount) % poolSize */
    private Dish pickDish(List<Dish> pool, int day, int mealIndex, int swapCount) {
        int idx = Math.floorMod(day * MEALS_PER_DAY + mealIndex + swapCount, pool.size());
        return pool.get(idx);
    }

    /** 打卡时同步写一条饮食记录 */
    private void insertPlanDietRecord(Long userId, UserPlanMeal meal) {
        Dish dish = dishMapper.findById(meal.getDishId());
        if (dish == null) return;
        DietRecord record = new DietRecord();
        record.setId(SnowflakeUtil.generate());
        record.setUserId(userId);
        record.setRecordDate(LocalDate.now());
        record.setRecordTime(LocalTime.now());
        record.setMealType(switch (meal.getMealIndex()) {
            case 0 -> 10;
            case 1 -> 20;
            default -> 30;
        });
        record.setFoodName(dish.getDishName() + "（计划餐）");
        record.setQuantity(1);
        record.setCalories(dish.getCalories());
        record.setProtein(dish.getProtein());
        record.setFat(dish.getFat());
        record.setCarbs(dish.getCarbs());
        record.setSourceType(DIET_SOURCE_PLAN);
        record.setPlanMealId(meal.getId());
        dietRecordMapper.insert(record);
    }

    private Map<String, Object> buildPlanDetail(UserPlan plan) {
        Map<String, Object> result = new HashMap<>();
        result.put("plan", plan);
        result.put("meals", userPlanMealMapper.findByPlanId(plan.getId()));
        return result;
    }

}
