package com.ws.bitesmart.mapper.user;

import com.ws.bitesmart.entity.user.UserAddress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户地址 Mapper
 *
 * is_default=1 表示默认地址，只能有一个。
 * resetDefault 在设置新默认地址前先把旧的取消。
 */
@Mapper
public interface UserAddressMapper {

    /** 按用户ID查地址列表，默认地址排最前 */
    List<UserAddress> findByUserId(@Param("userId") Long userId);

    /** 按ID查单个地址 */
    UserAddress findById(@Param("id") Long id);

    /** 新增地址 */
    int insert(UserAddress address);

    /** 更新地址（动态SQL，只改非空） */
    int updateById(UserAddress address);

    /** 把该用户所有地址的 is_default 设为 0 */
    int resetDefault(@Param("userId") Long userId);

    /** 软删除 */
    int deleteById(@Param("id") Long id);

}
