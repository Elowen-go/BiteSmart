package com.ws.bitesmart.mapper.delivery;

import com.ws.bitesmart.entity.delivery.RiderLocation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 骑手轨迹点 Mapper
 */
@Mapper
public interface RiderLocationMapper {

    int insert(RiderLocation location);

    /** 查某任务的轨迹点，按上报时间升序（用于轨迹回放） */
    List<RiderLocation> findByTaskId(@Param("taskId") Long taskId);

}
