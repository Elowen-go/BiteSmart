package com.ws.bitesmart.mapper.ai;

import com.ws.bitesmart.entity.ai.AiRecommendRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AI推荐规则 Mapper
 */
@Mapper
public interface AiRecommendRuleMapper {

    /** 按目标匹配最合适的规则（优先级最高的那条） */
    AiRecommendRule findBestRuleByGoal(@Param("targetGoal") String targetGoal);

    /** 查询所有规则 */
    List<AiRecommendRule> findAll();

    /** 新增规则 */
    int insert(AiRecommendRule rule);

    /** 更新规则 */
    int updateById(AiRecommendRule rule);

    /** 逻辑删除 */
    int deleteById(@Param("id") Long id);

}
