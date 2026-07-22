package com.ws.bitesmart.mapper.health;

import com.ws.bitesmart.entity.health.ExerciseLibrary;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 运动库 Mapper
 */
@Mapper
public interface ExerciseLibraryMapper {

    /** 全部运动项目，按 sort 升序 */
    List<ExerciseLibrary> findAll();

}
