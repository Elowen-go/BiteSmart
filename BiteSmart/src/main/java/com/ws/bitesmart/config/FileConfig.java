package com.ws.bitesmart.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 文件上传配置
 *
 * 负责：
 * 1. 读取 yml 中的文件上传配置
 * 2. 启动时自动创建上传目录
 * 3. 提供文件类型校验等方法
 */
@Slf4j
@Getter
@Configuration
public class FileConfig {

    /** 文件上传根目录（相对于项目根路径） */
    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    /** 单个文件大小上限（字节） */
    @Value("${file.max-size:10485760}")
    private long maxSize;

    /** 允许上传的文件MIME类型 */
    @Value("${file.allowed-types:image/jpeg,image/png,image/gif}")
    private String allowedTypes;

    /** 解析后的允许类型集合 */
    private Set<String> allowedTypeSet;

    /**
     * 启动时自动创建上传目录
     */
    @PostConstruct
    public void init() {
        // 解析允许的文件类型
        allowedTypeSet = new HashSet<>(Arrays.asList(allowedTypes.split(",")));

        // 创建上传目录
        try {
            Path dir = Paths.get(uploadDir);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
                log.info("文件上传目录已创建: {}", dir.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("创建文件上传目录失败: {}", e.getMessage());
        }
    }

    /**
     * 检查文件类型是否允许上传
     */
    public boolean isAllowedMimeType(String mimeType) {
        return allowedTypeSet.contains(mimeType);
    }

    /**
     * 检查文件大小是否允许
     */
    public boolean isAllowedSize(long fileSize) {
        return fileSize <= maxSize;
    }

    /**
     * 获取文件存储的完整路径
     * 按日期分目录存储，如：./uploads/2026/07/09/xxx.jpg
     */
    public Path resolveUploadPath(String relativePath) {
        return Paths.get(uploadDir).resolve(relativePath).normalize();
    }

}
