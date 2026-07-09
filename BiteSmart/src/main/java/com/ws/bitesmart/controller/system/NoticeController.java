package com.ws.bitesmart.controller.system;

import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.entity.system.Notice;
import com.ws.bitesmart.service.system.NoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公告接口（用户端，无需登录）
 *
 * GET /api/notices - 已发布的公告列表，前端首页展示用。
 * 路径已在 SecurityConfig 中配为白名单。
 */
@Slf4j
@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    /**
     * 已发布的公告列表
     * GET /api/notices
     */
    @GetMapping
    public ResultVO<List<Notice>> list() {
        List<Notice> list = noticeService.findPublished();
        return ResultVO.success(list);
    }

}
