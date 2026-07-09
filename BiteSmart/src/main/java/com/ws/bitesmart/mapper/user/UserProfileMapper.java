package com.ws.bitesmart.mapper.user;

import com.ws.bitesmart.entity.user.UserProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 健康档案 Mapper
 *
 * 一个用户只有一条档案，按 userId 查或者更新。
 */
@Mapper
public interface UserProfileMapper {

    /** 根据用户ID查档案 */
    UserProfile findByUserId(@Param("userId") Long userId);

    /** 新建档案 */
    int insert(UserProfile profile);

    /** 按用户ID更新（只改非空字段） */
    int updateByUserId(UserProfile profile);

}
