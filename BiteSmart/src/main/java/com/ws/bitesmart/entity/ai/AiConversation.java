package com.ws.bitesmart.entity.ai;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI对话记录表 实体类
 *
 * 对应 ai_conversation 表。
 * 每次问答存一条记录，session_id 用来关联多轮对话。
 */
@Data
public class AiConversation {

    private Long id;
    private Long userId;

    /** 会话ID，多轮对话用同一 session_id */
    private String sessionId;

    /** 用户提问 */
    private String question;

    /** AI回复内容 */
    private String answer;

    /** 使用的模型（默认 qwen-plus） */
    private String modelName;

    /** 输入Token数 */
    private Integer promptTokens;

    /** 输出Token数 */
    private Integer completionTokens;

    /** 总Token数 */
    private Integer totalTokens;

    /** 预估费用（元） */
    private BigDecimal cost;

    /** 响应耗时（毫秒） */
    private Integer responseTime;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;

}
