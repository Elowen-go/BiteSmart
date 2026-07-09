package com.ws.bitesmart.service.system;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ws.bitesmart.common.util.SnowflakeUtil;
import com.ws.bitesmart.entity.system.Notice;
import com.ws.bitesmart.mapper.system.NoticeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 公告服务
 *
 * 管理员管理公告的 CRUD 操作，以及用户端查询已发布公告。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeMapper noticeMapper;

    /**
     * 分页查询所有公告（管理员端）
     */
    public PageInfo<Notice> findAll(int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Notice> list = noticeMapper.findAll();
        return new PageInfo<>(list);
    }

    /**
     * 查询已发布的公告列表（用户端，不需要分页）
     */
    public List<Notice> findPublished() {
        return noticeMapper.findPublished();
    }

    /**
     * 根据 ID 查询
     */
    public Notice findById(Long id) {
        return noticeMapper.findById(id);
    }

    /**
     * 新增公告
     */
    @Transactional
    public void add(Notice notice) {
        notice.setId(SnowflakeUtil.generate());
        if (notice.getStatus() == null) {
            notice.setStatus(20); // 默认草稿
        }
        noticeMapper.insert(notice);
        log.info("新增公告: id={}, title={}", notice.getId(), notice.getTitle());
    }

    /**
     * 修改公告
     */
    @Transactional
    public void update(Notice notice) {
        noticeMapper.updateById(notice);
        log.info("更新公告: id={}", notice.getId());
    }

    /**
     * 删除公告（逻辑删除）
     */
    @Transactional
    public void delete(Long id) {
        noticeMapper.deleteById(id);
        log.info("删除公告: id={}", id);
    }

}
