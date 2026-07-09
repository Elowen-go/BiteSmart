package com.ws.bitesmart.mapper.ai;

import com.ws.bitesmart.entity.ai.AiConversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AI对话记录 Mapper
 */
@Mapper
public interface AiConversationMapper {

    /** 查某个会话的所有对话记录（按时间正序） */
    List<AiConversation> findBySessionId(@Param("sessionId") String sessionId);

    /** 查用户的所有历史消息 */
    List<AiConversation> findSessionsByUserId(@Param("userId") Long userId);

    /** 保存一次问答 */
    int insert(AiConversation conversation);

}
