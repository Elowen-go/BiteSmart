package com.ws.bitesmart.common;

import com.github.pagehelper.PageInfo;
import lombok.Data;

import java.util.List;

/**
 * 统一分页响应体
 */
@Data
public class PageResultVO<T> {

    private int code;
    private String message;
    private long timestamp;
    private PageData<T> data;

    private PageResultVO(int code, String message, PageData<T> data) {
        this.code = code;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
        this.data = data;
    }

    public static <T> PageResultVO<T> success(PageInfo<T> pageInfo) {
        PageData<T> pageData = new PageData<>();
        pageData.setList(pageInfo.getList());
        pageData.setTotal(pageInfo.getTotal());
        pageData.setPageNum(pageInfo.getPageNum());
        pageData.setPageSize(pageInfo.getPageSize());
        pageData.setPages(pageInfo.getPages());
        return new PageResultVO<>(200, "操作成功", pageData);
    }

    public static <T> PageResultVO<T> success(List<T> list, long total, int pageNum, int pageSize) {
        PageData<T> pageData = new PageData<>();
        pageData.setList(list);
        pageData.setTotal(total);
        pageData.setPageNum(pageNum);
        pageData.setPageSize(pageSize);
        pageData.setPages((int) (total + pageSize - 1) / pageSize);
        return new PageResultVO<>(200, "操作成功", pageData);
    }

    @Data
    public static class PageData<T> {
        private List<T> list;
        private long total;
        private int pageNum;
        private int pageSize;
        private int pages;
    }

}
