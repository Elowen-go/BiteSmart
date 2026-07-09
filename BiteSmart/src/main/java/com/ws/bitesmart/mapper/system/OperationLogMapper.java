package com.ws.bitesmart.mapper.system;

import com.ws.bitesmart.entity.system.OperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作日志 Mapper
 */
@Mapper
public interface OperationLogMapper {

    int insert(OperationLog log);

    /** 查全部操作日志（分页） */
    java.util.List<OperationLog> findAll();

}
