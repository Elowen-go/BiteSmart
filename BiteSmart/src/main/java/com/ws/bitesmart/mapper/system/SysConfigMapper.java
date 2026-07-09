package com.ws.bitesmart.mapper.system;

import com.ws.bitesmart.entity.system.SysConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统配置 Mapper
 */
@Mapper
public interface SysConfigMapper {

    List<SysConfig> findAll();

    SysConfig findByKey(@Param("configKey") String configKey);

    List<SysConfig> findByGroup(@Param("configGroup") String configGroup);

    int insert(SysConfig config);

    int updateByKey(@Param("configKey") String configKey, @Param("configValue") String configValue,
                    @Param("description") String description, @Param("isSensitive") Integer isSensitive);

    int deleteById(@Param("id") Long id);

}
