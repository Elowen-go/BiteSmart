package com.ws.bitesmart.entity.user;

import lombok.Data;

import java.time.LocalDateTime;

/** Third-party identity binding for a BiteSmart user. */
@Data
public class UserThirdPartyIdentity {

    private Long id;
    private Long userId;
    private String provider;
    private String openId;
    private String unionId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
