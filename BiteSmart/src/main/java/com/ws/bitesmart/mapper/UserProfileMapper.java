package com.ws.bitesmart.mapper;

import com.ws.bitesmart.entity.UserProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserProfileMapper {

    UserProfile findByUserId(@Param("userId") Long userId);

    int insert(UserProfile profile);

    int updateByUserId(UserProfile profile);

}
