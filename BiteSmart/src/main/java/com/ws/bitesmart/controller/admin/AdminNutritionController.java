package com.ws.bitesmart.controller.admin;

import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.common.util.SnowflakeUtil;
import com.ws.bitesmart.entity.ai.NutritionStandard;
import com.ws.bitesmart.mapper.ai.NutritionStandardMapper;
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
 * 管理员端 - 营养标准管理
 *
 * 管理员管理营养膳食标准的 CRUD 操作。
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/nutrition")
@RequiredArgsConstructor
public class AdminNutritionController {

    private final NutritionStandardMapper nutritionStandardMapper;

    /**
     * 营养标准列表
     * GET /api/admin/nutrition
     */
    @GetMapping
    public ResultVO<List<NutritionStandard>> list() {
        return ResultVO.success(nutritionStandardMapper.findAll());
    }

    /**
     * 新增营养标准
     * POST /api/admin/nutrition
     */
    @PostMapping
    public ResultVO<Void> add(@RequestBody NutritionStandard standard) {
        standard.setId(SnowflakeUtil.generate());
        nutritionStandardMapper.insert(standard);
        log.info("新增营养标准: id={}, name={}", standard.getId(), standard.getStandardName());
        return ResultVO.ok("新增成功");
    }

    /**
     * 修改营养标准
     * PUT /api/admin/nutrition/{id}
     */
    @PutMapping("/{id}")
    public ResultVO<Void> update(@PathVariable Long id, @RequestBody NutritionStandard standard) {
        standard.setId(id);
        nutritionStandardMapper.updateById(standard);
        log.info("更新营养标准: id={}", id);
        return ResultVO.ok("修改成功");
    }

    /**
     * 删除营养标准（逻辑删除）
     * DELETE /api/admin/nutrition/{id}
     */
    @DeleteMapping("/{id}")
    public ResultVO<Void> delete(@PathVariable Long id) {
        nutritionStandardMapper.deleteById(id);
        log.info("删除营养标准: id={}", id);
        return ResultVO.ok("删除成功");
    }

}
