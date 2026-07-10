package com.ws.bitesmart.entity.system;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Notice {

    private Long id;

    private String title;

    private String content;

    private Integer noticeType;

    private Integer targetRole;

    private Integer priority;

    private Integer status;

    private LocalDateTime publishTime;

    private LocalDateTime expireTime;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;

}