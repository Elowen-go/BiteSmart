package com.ws.bitesmart.controller.admin;

import com.ws.bitesmart.common.PageResultVO;
import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.common.util.SnowflakeUtil;
import com.ws.bitesmart.entity.system.OperationLog;
import com.ws.bitesmart.entity.system.SysConfig;
import com.ws.bitesmart.mapper.system.OperationLogMapper;
import com.ws.bitesmart.mapper.system.SysConfigMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理员 - 系统管理
 *
 * 系统配置管理 + 操作日志查询。
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/system")
@RequiredArgsConstructor
public class AdminSystemController {

    private final SysConfigMapper sysConfigMapper;
    private final OperationLogMapper operationLogMapper;

    // ==================== 系统配置 ====================

    @GetMapping("/configs")
    public ResultVO<List<SysConfig>> listConfigs(@RequestParam(required = false) String group) {
        if (group != null) {
            return ResultVO.success(sysConfigMapper.findByGroup(group));
        }
        return ResultVO.success(sysConfigMapper.findAll());
    }

    @PostMapping("/configs")
    public ResultVO<Void> addConfig(@RequestBody SysConfig config) {
        config.setId(SnowflakeUtil.generate());
        sysConfigMapper.insert(config);
        return ResultVO.ok("新增成功");
    }

    @PutMapping("/configs/{configKey}")
    public ResultVO<Void> updateConfig(@PathVariable String configKey,
                                        @RequestParam String configValue,
                                        @RequestParam(required = false) String description,
                                        @RequestParam(required = false, defaultValue = "0") Integer isSensitive) {
        sysConfigMapper.updateByKey(configKey, configValue, description, isSensitive);
        return ResultVO.ok("修改成功");
    }

    @DeleteMapping("/configs/{id}")
    public ResultVO<Void> deleteConfig(@PathVariable Long id) {
        sysConfigMapper.deleteById(id);
        return ResultVO.ok("删除成功");
    }

    // ==================== 操作日志 ====================

    @GetMapping("/logs")
    public PageResultVO<OperationLog> listLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageHelper.startPage(page, size);
        List<OperationLog> list = operationLogMapper.findAll();
        PageInfo<OperationLog> pageInfo = new PageInfo<>(list);
        return PageResultVO.success(pageInfo);
    }

}
