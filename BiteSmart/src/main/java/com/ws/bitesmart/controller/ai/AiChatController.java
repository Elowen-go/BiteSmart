package com.ws.bitesmart.controller.ai;

import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.entity.ai.AiConversation;
import com.ws.bitesmart.security.LoginUser;
import com.ws.bitesmart.service.ai.AiChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * AI智能问答接口
 *
 * 用户可以和 AI 进行多轮对话，支持上下文关联。
 * 首次对话不用传 sessionId，后端自动生成。
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;

    /**
     * AI对话
     * POST /api/ai/chat?question=xxx&sessionId=xxx
     *
     * @param question  用户提问
     * @param sessionId 会话ID，首次传空或传新值
     */
    @PostMapping("/chat")
    public ResultVO<AiConversation> chat(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam String question,
            @RequestParam(required = false) String sessionId) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        AiConversation conversation = aiChatService.chat(loginUser.getUserId(), sessionId, question);
        return ResultVO.success(conversation);
    }

    /**
     * 流式 AI 对话。每个 token 通过 SSE 推送到前端，完整答案仍会保存到会话记录。
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam String question,
            @RequestParam(required = false) String sessionId) {
        SseEmitter emitter = new SseEmitter(180_000L);
        if (loginUser == null) {
            emitter.completeWithError(new IllegalStateException("未登录"));
            return emitter;
        }
        aiChatService.chatStream(loginUser.getUserId(), sessionId, question, emitter);
        return emitter;
    }

    /**
     * 获取某个会话的历史记录
     * GET /api/ai/chat/history?sessionId=xxx
     */
    @GetMapping("/chat/history")
    public ResultVO<List<AiConversation>> getHistory(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam String sessionId) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        List<AiConversation> history = aiChatService.getHistory(loginUser.getUserId(), sessionId);
        return ResultVO.success(history);
    }

    /** 获取当前登录用户的历史会话消息，前端按 sessionId 聚合为会话列表。 */
    @GetMapping("/chat/sessions")
    public ResultVO<List<AiConversation>> getSessions(
            @AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) return ResultVO.error(401, "未登录");
        return ResultVO.success(aiChatService.getUserMessages(loginUser.getUserId()));
    }

}
