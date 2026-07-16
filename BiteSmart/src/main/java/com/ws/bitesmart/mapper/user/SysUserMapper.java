package com.ws.bitesmart.mapper.user;

import com.ws.bitesmart.entity.user.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统用户 Mapper
 *
 * 对应 sys_user 表的基本操作。
 * 主要查询按 username（登录用）和按id（查详情用）来写。
 */
@Mapper
public interface SysUserMapper {

    /**
     * 根据用户名查用户
     * 登录的时候用，账号密码登录和用户名唯一性检查都靠这个
     */
    SysUser findByUsername(@Param("username") String username);

    /** 按用户名或手机号查询登录账号。 */
    SysUser findByUsernameOrPhone(@Param("account") String account);

    /** 按手机号查询账号，用于完善账号时的唯一性校验。 */
    SysUser findByPhone(@Param("phone") String phone);

    /**
     * 根据ID查用户
     */
    SysUser findById(@Param("id") Long id);

    /**
     * 新增用户
     * 返回影响行数，1表示成功
     */
    int insert(SysUser user);

    /**
     * 更新用户信息
     * 只更新不为空的字段（动态SQL）
     */
    int updateById(SysUser user);

    /** 更新微信临时账号的用户名、手机号和密码。 */
    int updateCredentials(@Param("id") Long id,
                          @Param("username") String username,
                          @Param("phone") String phone,
                          @Param("password") String password);

    /**
     * 更新最后登录时间
     */
    int updateLoginTime(@Param("id") Long id, @Param("lastLoginTime") java.time.LocalDateTime time, @Param("lastLoginIp") String ip);

    /**
     * 分页查询所有非删除用户
     */
    List<SysUser> findAll();

    /**
     * 统计用户总数
     */
    long countAll();

}
