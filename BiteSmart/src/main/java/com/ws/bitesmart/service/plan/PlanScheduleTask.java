package com.ws.bitesmart.service.plan;

import com.ws.bitesmart.mapper.plan.UserPlanMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 专属计划定时任务
 *
 * 每天 00:30 把所有进行中（status=20）的计划推进一天：
 * cur_day+1 未超过 plan_days 则 cur_day+1，否则置为已完成（status=30）。
 * 与手动演示接口 POST /api/plan/advance 互不冲突（各管各的推进）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlanScheduleTask {

    private final UserPlanMapper userPlanMapper;

    /** 每天 00:30 执行（秒 分 时 日 月 周） */
    @Scheduled(cron = "0 30 0 * * *")
    public void advanceRunningPlans() {
        int count = userPlanMapper.advanceRunningPlans();
        log.info("计划自动跨天任务执行完成：共处理 {} 份进行中的计划", count);
    }

}
