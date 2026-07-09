package com.ws.bitesmart.controller.health;

import com.ws.bitesmart.common.PageResultVO;
import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.entity.health.DietRecord;
import com.ws.bitesmart.entity.health.ExerciseRecord;
import com.ws.bitesmart.entity.health.WeightRecord;
import com.ws.bitesmart.security.LoginUser;
import com.ws.bitesmart.service.health.HealthRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 健康记录接口
 *
 * 饮食、运动、体重 三类健康数据的 CRUD。
 * 路径统一在 /api/health 下，按记录类型区分：
 *   /api/health/diet      饮食记录
 *   /api/health/exercise  运动记录
 *   /api/health/weight    体重记录
 */
@Slf4j
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthRecordController {

    private final HealthRecordService healthRecordService;

    // ==================== 饮食记录 ====================

    @GetMapping("/diet")
    public ResultVO<List<DietRecord>> getDietRecords(@AuthenticationPrincipal LoginUser loginUser,
                                                      @RequestParam(required = false) LocalDate date) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        if (date != null) {
            return ResultVO.success(healthRecordService.getDietRecordsByDate(loginUser.getUserId(), date));
        }
        return ResultVO.success(healthRecordService.getDietRecords(loginUser.getUserId()));
    }

    @PostMapping("/diet")
    public ResultVO<Void> addDiet(@AuthenticationPrincipal LoginUser loginUser,
                                   @RequestBody DietRecord record) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        healthRecordService.addDietRecord(loginUser.getUserId(), record);
        return ResultVO.ok("添加成功");
    }

    @PutMapping("/diet/{id}")
    public ResultVO<Void> updateDiet(@AuthenticationPrincipal LoginUser loginUser,
                                      @PathVariable Long id,
                                      @RequestBody DietRecord record) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        record.setId(id);
        healthRecordService.updateDietRecord(loginUser.getUserId(), record);
        return ResultVO.ok("更新成功");
    }

    @DeleteMapping("/diet/{id}")
    public ResultVO<Void> deleteDiet(@AuthenticationPrincipal LoginUser loginUser,
                                      @PathVariable Long id) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        healthRecordService.deleteDietRecord(loginUser.getUserId(), id);
        return ResultVO.ok("删除成功");
    }

    // ==================== 运动记录 ====================

    @GetMapping("/exercise")
    public ResultVO<List<ExerciseRecord>> getExerciseRecords(@AuthenticationPrincipal LoginUser loginUser,
                                                              @RequestParam(required = false) LocalDate date) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        if (date != null) {
            return ResultVO.success(healthRecordService.getExerciseRecordsByDate(loginUser.getUserId(), date));
        }
        return ResultVO.success(healthRecordService.getExerciseRecords(loginUser.getUserId()));
    }

    @PostMapping("/exercise")
    public ResultVO<Void> addExercise(@AuthenticationPrincipal LoginUser loginUser,
                                       @RequestBody ExerciseRecord record) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        healthRecordService.addExerciseRecord(loginUser.getUserId(), record);
        return ResultVO.ok("添加成功");
    }

    @DeleteMapping("/exercise/{id}")
    public ResultVO<Void> deleteExercise(@AuthenticationPrincipal LoginUser loginUser,
                                          @PathVariable Long id) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        healthRecordService.deleteExerciseRecord(loginUser.getUserId(), id);
        return ResultVO.ok("删除成功");
    }

    // ==================== 体重记录 ====================

    @GetMapping("/weight")
    public ResultVO<?> getWeightRecords(@AuthenticationPrincipal LoginUser loginUser,
                                         @RequestParam(required = false) Integer page,
                                         @RequestParam(defaultValue = "10") int size) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        if (page != null) {
            return ResultVO.success(PageResultVO.success(healthRecordService.getWeightRecords(loginUser.getUserId(), page, size)));
        }
        return ResultVO.success(healthRecordService.getWeightRecords(loginUser.getUserId()));
    }

    @PostMapping("/weight")
    public ResultVO<Void> saveWeight(@AuthenticationPrincipal LoginUser loginUser,
                                      @RequestBody WeightRecord record) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        healthRecordService.saveWeightRecord(loginUser.getUserId(), record);
        return ResultVO.ok("保存成功");
    }

}
