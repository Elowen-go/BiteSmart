package com.ws.bitesmart.controller.admin;

import com.ws.bitesmart.common.PageResultVO;
import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.entity.system.Notice;
import com.ws.bitesmart.service.system.NoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员端 - 公告管理
 *
 * 公告的 CRUD 操作，仅管理员可访问。
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/notices")
@RequiredArgsConstructor
public class AdminNoticeController {

    private final NoticeService noticeService;

    /**
     * 公告列表（分页）
     * GET /api/admin/notices?pageNum=1&pageSize=10
     */
    @GetMapping
    public PageResultVO<Notice> list(@RequestParam(defaultValue = "1") int pageNum,
                                     @RequestParam(defaultValue = "10") int pageSize) {
        return PageResultVO.success(noticeService.findAll(pageNum, pageSize));
    }

    /**
     * 公告详情
     * GET /api/admin/notices/{id}
     */
    @GetMapping("/{id}")
    public ResultVO<Notice> detail(@PathVariable Long id) {
        Notice notice = noticeService.findById(id);
        if (notice == null) {
            return ResultVO.error(404, "公告不存在");
        }
        return ResultVO.success(notice);
    }

    /**
     * 新增公告
     * POST /api/admin/notices
     */
    @PostMapping
    public ResultVO<Void> add(@RequestBody Notice notice) {
        noticeService.add(notice);
        return ResultVO.ok("新增成功");
    }

    /**
     * 修改公告
     * PUT /api/admin/notices/{id}
     */
    @PutMapping("/{id}")
    public ResultVO<Void> update(@PathVariable Long id, @RequestBody Notice notice) {
        notice.setId(id);
        noticeService.update(notice);
        return ResultVO.ok("修改成功");
    }

    /**
     * 删除公告
     * DELETE /api/admin/notices/{id}
     */
    @DeleteMapping("/{id}")
    public ResultVO<Void> delete(@PathVariable Long id) {
        noticeService.delete(id);
        return ResultVO.ok("删除成功");
    }

}
