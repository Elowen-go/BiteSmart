package com.ws.bitesmart.mapper.user;

import com.ws.bitesmart.entity.user.UserThirdPartyIdentity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserThirdPartyIdentityMapper {

    UserThirdPartyIdentity findByProviderAndOpenId(@Param("provider") String provider,
                                                   @Param("openId") String openId);

    int insert(UserThirdPartyIdentity identity);
}
