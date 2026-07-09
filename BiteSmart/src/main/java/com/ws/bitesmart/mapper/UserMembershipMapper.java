package com.ws.bitesmart.mapper;

import com.ws.bitesmart.entity.UserMembership;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMembershipMapper {

    /** 当前生效中的会员（status=10 且 end_time > 现在） */
    UserMembership findActiveByUserId(@Param("userId") Long userId);

    List<UserMembership> findByUserId(@Param("userId") Long userId);

    int insert(UserMembership membership);

}
