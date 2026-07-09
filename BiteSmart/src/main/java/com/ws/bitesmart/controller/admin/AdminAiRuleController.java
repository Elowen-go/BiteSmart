package com.ws.bitesmart.controller.admin;

import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.common.util.SnowflakeUtil;
import com.ws.bitesmart.entity.ai.AiRecommendRule;
import com.ws.bitesmart.mapper.ai.AiRecommendRuleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理员端 - AI推荐规则管理
 *
 * 管理员管理 AI 推荐规则的 CRUD 操作。
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/ai-rules")
@RequiredArgsConstructor
public class AdminAiRuleController {

    private final AiRecommendRuleMapper aiRecommendRuleMapper;

    /**
     * 规则列表
     * GET /api/admin/ai-rules
     */
    @GetMapping
    public ResultVO<List<AiRecommendRule>> list() {
        return ResultVO.success(aiRecommendRuleMapper.findAll());
    }

    /**
     * 新增规则
     * POST /api/admin/ai-rules
     */
    @PostMapping
    public ResultVO<Void> add(@RequestBody AiRecommendRule rule) {
        rule.setId(SnowflakeUtil.generate());
        aiRecommendRuleMapper.insert(rule);
        log.info("新增AI推荐规则: id={}, name={}", rule.getId(), rule.getRuleName());
        return ResultVO.ok("新增成功");
    }

    /**
     * 修改规则
     * PUT /api/admin/ai-rules/{id}
     */
    @PutMapping("/{id}")
    public ResultVO<Void> update(@PathVariable Long id, @RequestBody AiRecommendRule rule) {
        rule.setId(id);
        aiRecommendRuleMapper.updateById(rule);
        log.info("更新AI推荐规则: id={}", id);
        return ResultVO.ok("修改成功");
    }

    /**
     * 删除规则（逻辑删除）
     * DELETE /api/admin/ai-rules/{id}
     */
    @DeleteMapping("/{id}")
    public ResultVO<Void> delete(@PathVariable Long id) {
        aiRecommendRuleMapper.deleteById(id);
        log.info("删除AI推荐规则: id={}", id);
        return ResultVO.ok("删除成功");
    }

}
