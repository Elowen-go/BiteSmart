package com.ws.bitesmart.mapper.user;

import com.ws.bitesmart.entity.user.UserMembership;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户会员记录 Mapper
 *
 * 查找生效中的会员需要同时判断 status=10 和 end_time > NOW()
 */
@Mapper
public interface UserMembershipMapper {

    /** 当前生效中的会员（status=10 且 end_time > 现在） */
    UserMembership findActiveByUserId(@Param("userId") Long userId);

    /** 所有会员记录（含已过期的），按时间倒序 */
    List<UserMembership> findByUserId(@Param("userId") Long userId);

    /** 新增会员记录 */
    int insert(UserMembership membership);

}
