package com.ws.bitesmart.service.ai;

import com.ws.bitesmart.common.util.SnowflakeUtil;
import com.ws.bitesmart.entity.ai.AiConversation;
import com.ws.bitesmart.mapper.ai.AiConversationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * AI对话服务
 *
 * 目前是 mock 实现，返回固定回复。
 * 后续接入真实 AI 模型后，替换 callAiApi() 方法即可。
 *
 * 多轮对话机制：
 *   前端每次请求传同一个 sessionId，后台用 sessionId 查历史记录，
 *   拼到 prompt 里实现上下文关联。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final AiConversationMapper aiConversationMapper;

    /**
     * AI 对话
     *
     * @param userId    当前用户ID
     * @param sessionId 会话ID（首次传空，后端自动生成）
     * @param question  用户提问
     * @return 本次问答记录（含AI回复）
     */
    @Transactional
    public AiConversation chat(Long userId, String sessionId, String question) {
        // 首次对话自动生成 sessionId
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString().replace("-", "");
        }

        // 记录开始时间
        long startTime = System.currentTimeMillis();

        // 调用 AI 模型获取回复（目前是 mock）
        String answer = callAiApi(question, userId, sessionId);

        // 计算耗时和 token
        long responseTime = System.currentTimeMillis() - startTime;

        // 保存到数据库
        AiConversation conversation = new AiConversation();
        conversation.setId(SnowflakeUtil.generate());
        conversation.setUserId(userId);
        conversation.setSessionId(sessionId);
        conversation.setQuestion(question);
        conversation.setAnswer(answer);
        conversation.setModelName("mock-model");
        conversation.setPromptTokens(question.length());
        conversation.setCompletionTokens(answer.length());
        conversation.setTotalTokens(question.length() + answer.length());
        conversation.setCost(BigDecimal.ZERO);
        conversation.setResponseTime((int) responseTime);

        aiConversationMapper.insert(conversation);

        log.info("AI对话: userId={}, sessionId={}, 耗时={}ms", userId, sessionId, responseTime);
        return conversation;
    }

    /**
     * 查某个会话的历史记录
     */
    public List<AiConversation> getHistory(String sessionId) {
        return aiConversationMapper.findBySessionId(sessionId);
    }

    /**
     * 查用户的所有历史消息
     */
    public List<AiConversation> getUserMessages(Long userId) {
        return aiConversationMapper.findSessionsByUserId(userId);
    }

    /**
     * 调用 AI 模型接口
     *
     * 当前是 mock 实现，返回简单的模拟回复。
     * TODO: 接入真实 AI API（如通义千问、文心一言等）
     */
    private String callAiApi(String question, Long userId, String sessionId) {
        // ===== 临时 mock 实现 =====
        // 后续替换为真实 HTTP 调用：
        // 1. 从 yml 读取 AI 接口地址和 key
        // 2. 构建请求参数（含历史上下文）
        // 3. 发送 HTTP 请求
        // 4. 解析返回结果

        if (question.contains("你好") || question.contains("hello")) {
            return "你好！我是健康膳食助手，可以帮你制定饮食计划、推荐健康食谱、解答营养问题。请问有什么需要帮助的吗？";
        }
        if (question.contains("减肥") || question.contains("减脂")) {
            return "关于减肥，我建议你注意以下几点：\n\n1. 控制总热量摄入，建议每天减少 300-500 大卡\n2. 增加蛋白质摄入，保持肌肉量\n3. 选择低GI的碳水来源\n4. 每周进行 3-5 次有氧运动\n5. 保证每天 7-8 小时睡眠\n\n需要我为你生成一份具体的减肥食谱吗？";
        }
        if (question.contains("增肌")) {
            return "关于增肌，核心原则是：热量盈余 + 足量蛋白质 + 力量训练。\n\n建议：\n1. 每天热量摄入比消耗多 300-500 大卡\n2. 蛋白质摄入量 1.6-2.2g/kg 体重\n3. 碳水化合物占 50-60%\n4. 每周 3-4 次力量训练\n5. 训练后及时补充蛋白质\n\n需要我为你定制增肌食谱吗？";
        }
        if (question.contains("蛋白") || question.contains("营养")) {
            return "关于蛋白质摄入，一般建议：\n\n- 普通成年人：0.8-1.0g/kg 体重\n- 运动人群：1.2-1.6g/kg 体重\n- 增肌人群：1.6-2.2g/kg 体重\n\n优质蛋白来源：鸡胸肉、鱼虾、鸡蛋、牛奶、豆制品。\n\n你可以先在健康档案里填写个人信息，我可以帮你计算每日所需营养。";
        }
        // 默认回复
        return "感谢你的提问！我是健康膳食助手，可以帮你：\n\n1. 生成个性化每日食谱\n2. 推荐适合你的健康套餐\n3. 解答营养和饮食问题\n4. 制定减肥/增肌饮食计划\n\n你可以告诉我具体的需求，我会尽力帮助你！";
    }

}
