package com.ws.bitesmart.service.system;

import com.ws.bitesmart.common.util.SnowflakeUtil;
import com.ws.bitesmart.entity.system.OperationLog;
import com.ws.bitesmart.mapper.system.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 操作日志服务
 *
 * 关键业务操作完成后调用 record() 方法记录日志。
 * 调用示例：
 *   operateLogService.record(userId, username, roleType, "创建订单", null, null, null, null, null, null);
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperateLogService {

    private final OperationLogMapper operationLogMapper;

    /**
     * 记录操作日志
     */
    public void record(Long userId, String username, Integer roleType,
                       String operation, String method, String requestUrl,
                       String requestParams, String requestIp,
                       Integer costTime, String errorMsg) {
        try {
            OperationLog log = new OperationLog();
            log.setId(SnowflakeUtil.generate());
            log.setUserId(userId);
            log.setUsername(username);
            log.setRoleType(roleType);
            log.setOperation(operation);
            log.setMethod(method);
            log.setRequestUrl(requestUrl);
            log.setRequestParams(requestParams);
            log.setRequestIp(requestIp);
            log.setCostTime(costTime);
            log.setStatus(errorMsg == null ? 10 : 20);
            log.setErrorMsg(errorMsg);
            operationLogMapper.insert(log);
        } catch (Exception e) {
            // 日志记录失败不影响主业务，只打印警告
            log.warn("操作日志记录失败: {}", e.getMessage());
        }
    }

}
