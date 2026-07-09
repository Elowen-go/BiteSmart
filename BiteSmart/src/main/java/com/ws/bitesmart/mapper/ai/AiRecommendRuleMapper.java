package com.ws.bitesmart.mapper.ai;

import com.ws.bitesmart.entity.ai.AiRecommendRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * AI推荐规则 Mapper
 */
@Mapper
public interface AiRecommendRuleMapper {

    /** 按目标匹配最合适的规则（优先级最高的那条） */
    AiRecommendRule findBestRuleByGoal(@Param("targetGoal") String targetGoal);

}
