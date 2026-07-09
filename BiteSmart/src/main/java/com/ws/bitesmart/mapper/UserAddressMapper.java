package com.ws.bitesmart.mapper;

import com.ws.bitesmart.entity.UserAddress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserAddressMapper {

    List<UserAddress> findByUserId(@Param("userId") Long userId);

    UserAddress findById(@Param("id") Long id);

    int insert(UserAddress address);

    int updateById(UserAddress address);

    /** 把该用户所有地址的 is_default 设为 0 */
    int resetDefault(@Param("userId") Long userId);

    int deleteById(@Param("id") Long id);

}
