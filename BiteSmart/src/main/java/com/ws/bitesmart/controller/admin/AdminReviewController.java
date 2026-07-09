package com.ws.bitesmart.controller.admin;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ws.bitesmart.common.PageResultVO;
import com.ws.bitesmart.common.ResultVO;
import com.ws.bitesmart.entity.review.Review;
import com.ws.bitesmart.mapper.review.ReviewMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理员端 - 评论管理
 *
 * 管理员查看全部评论，隐藏/显示评论。
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/reviews")
@RequiredArgsConstructor
public class AdminReviewController {

    private final ReviewMapper reviewMapper;

    /**
     * 全部评论（分页）
     * GET /api/admin/reviews?pageNum=1&pageSize=10
     */
    @GetMapping
    public PageResultVO<Review> list(@RequestParam(defaultValue = "1") int pageNum,
                                      @RequestParam(defaultValue = "10") int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Review> list = reviewMapper.findAll();
        return PageResultVO.success(new PageInfo<>(list));
    }

    /**
     * 隐藏/显示评论
     * PUT /api/admin/reviews/{id}/status?status=20
     * status: 10-已发布 20-已隐藏
     */
    @PutMapping("/{id}/status")
    public ResultVO<Void> updateStatus(@PathVariable Long id,
                                       @RequestParam Integer status) {
        reviewMapper.updateStatus(id, status);
        log.info("管理员变更评论状态: reviewId={}, status={}", id, status);
        return ResultVO.ok(status == 10 ? "评论已显示" : "评论已隐藏");
    }

}
