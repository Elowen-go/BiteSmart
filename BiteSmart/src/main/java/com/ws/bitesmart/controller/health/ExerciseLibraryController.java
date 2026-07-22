package com.ws.bitesmart.controller.health;

import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.entity.health.ExerciseLibrary;
import com.ws.bitesmart.mapper.health.ExerciseLibraryMapper;
import com.ws.bitesmart.security.LoginUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 运动库接口
 *
 * 平台预置的运动项目库，小程序"运动库"页按分类浏览，
 * 选中后按 kcalPerMin × 时长 估算消耗并写入 /api/health/exercise。
 */
@Slf4j
@RestController
@RequestMapping("/api/exercise")
@RequiredArgsConstructor
public class ExerciseLibraryController {

    private final ExerciseLibraryMapper exerciseLibraryMapper;

    /**
     * 获取运动库全部项目（按 sort 排序）
     * GET /api/exercise/library
     */
    @GetMapping("/library")
    public ResultVO<List<ExerciseLibrary>> library(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        return ResultVO.success(exerciseLibraryMapper.findAll());
    }

}
