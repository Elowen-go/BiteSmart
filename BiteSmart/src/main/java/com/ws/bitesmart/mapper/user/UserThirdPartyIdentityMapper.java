package com.ws.bitesmart.mapper.user;

import com.ws.bitesmart.entity.user.UserThirdPartyIdentity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserThirdPartyIdentityMapper {

    UserThirdPartyIdentity findByProviderAndOpenId(@Param("provider") String provider,
                                                   @Param("openId") String openId);

    UserThirdPartyIdentity findByUserIdAndProvider(@Param("userId") Long userId,
                                                   @Param("provider") String provider);

    int insert(UserThirdPartyIdentity identity);
}
